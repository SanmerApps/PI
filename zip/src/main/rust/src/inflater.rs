use crate::typed_call;
use bytes::{BufMut, BytesMut};
use std::cell::RefCell;
use std::ops::{Deref, DerefMut};
use typed_jni::core::JNIEnv;
use typed_jni::{
    Array, TrampolineClass, TrampolineObject, TypedPrimitiveArrayExt, TypedThrowableExt, define_java_class,
};
use zlib_rs::c_api::z_stream;
use zlib_rs::inflate::InflateStream;
use zlib_rs::{InflateConfig, InflateFlush, ReturnCode};

define_java_class!(JavaInflater, "dev.sanmer.zip.Inflater$Native");

#[repr(transparent)]
struct ZStream(z_stream);

impl ZStream {
    fn as_mut(&mut self) -> &mut InflateStream<'_> {
        unsafe { &mut *(&mut self.0 as *mut z_stream as *mut _) }
    }
}

impl Deref for ZStream {
    type Target = z_stream;

    fn deref(&self) -> &Self::Target {
        &self.0
    }
}

impl DerefMut for ZStream {
    fn deref_mut(&mut self) -> &mut Self::Target {
        &mut self.0
    }
}

impl Drop for ZStream {
    fn drop(&mut self) {
        let _ = zlib_rs::inflate::end(self.as_mut());
    }
}

thread_local! {
    static STREAM: RefCell<Option<ZStream>> = const { RefCell::new(None) }
}

macro_rules! get {
    ($($name:ident -> $t:ty = $default:expr),* $(,)?) => {
        $(pub extern "system" fn $name() -> $t {
            STREAM.with_borrow(|stream| {
                let Some(stream) = stream else {
                    return $default;
                };
                stream.$name
            })
        })*
    };
}

get! {
    avail_in -> u32 = 0,
    avail_out -> u32 = 0,
    total_in -> u64 = 0,
    total_out -> u64 = 0,
    data_type -> i32 = 0,
}

pub extern "system" fn init(bits: i32) -> i32 {
    STREAM.with_borrow_mut(|stream| match stream {
        None => {
            let mut z_stream = z_stream::default();
            let config = InflateConfig { window_bits: bits };
            let ret = zlib_rs::inflate::init(&mut z_stream, config);
            if ret == ReturnCode::Ok {
                stream.replace(ZStream(z_stream));
            }
            ret
        }
        Some(stream) => {
            let config = InflateConfig { window_bits: bits };
            zlib_rs::inflate::reset_with_config(stream.as_mut(), config)
        }
    }) as i32
}

pub extern "system" fn prime(bits: i32, value: i32) -> i32 {
    STREAM.with_borrow_mut(|stream| {
        let Some(stream) = stream else {
            return ReturnCode::StreamError as i32;
        };
        let ret = zlib_rs::inflate::prime(stream.as_mut(), bits, value);
        ret as i32
    })
}

pub extern "system" fn set_dictionary<'env>(
    env: &'env JNIEnv,
    _cls: TrampolineClass<'env, JavaInflater>,
    bytes: TrampolineObject<'env, Array<i8>>,
    offset: i32,
    length: u32,
) -> i32 {
    STREAM.with_borrow_mut(|stream| {
        let Some(stream) = stream else {
            return ReturnCode::StreamError as i32;
        };
        let mut buf = Box::<[u8]>::new_uninit_slice(length as usize);
        typed_call!(env, env.typed_get_bytes_array_region(&bytes, offset, &mut buf));
        let buf = unsafe { buf.assume_init() };
        let ret = zlib_rs::inflate::set_dictionary(stream.as_mut(), &buf);
        ret as i32
    })
}

pub extern "system" fn inflate<'env>(
    env: &'env JNIEnv,
    _cls: TrampolineClass<'env, JavaInflater>,
    in_bytes: TrampolineObject<'env, Array<i8>>,
    in_offset: i32,
    in_length: u32,
    out_bytes: TrampolineObject<'env, Array<i8>>,
    out_offset: i32,
    out_length: u32,
    flush: i32,
) -> i32 {
    STREAM.with_borrow_mut(|stream| {
        let Some(stream) = stream else {
            return ReturnCode::StreamError as i32;
        };

        let mut in_buf = Box::<[u8]>::new_uninit_slice(in_length as usize);
        typed_call!(env, env.typed_get_bytes_array_region(&in_bytes, in_offset, &mut in_buf));
        let in_buf = unsafe { in_buf.assume_init() };
        stream.next_in = in_buf.as_ptr();
        stream.avail_in = in_length;

        let mut out_buf = BytesMut::with_capacity(out_length as usize);
        stream.next_out = out_buf.chunk_mut().as_mut_ptr();
        stream.avail_out = out_length;

        let flush = InflateFlush::try_from(flush).unwrap_or_default();
        let ret = unsafe { zlib_rs::inflate::inflate(stream.as_mut(), flush) };
        let produced = out_length - stream.avail_out;
        unsafe { out_buf.advance_mut(produced as usize) };

        typed_call!(env, env.typed_set_bytes_array_region(&out_bytes, out_offset, &out_buf));
        ret as i32
    })
}
