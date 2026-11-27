use crate::typed_call;
use typed_jni::core::JNIEnv;
use typed_jni::{
    Array, TrampolineClass, TrampolineObject, TypedPrimitiveArrayExt, TypedThrowableExt, define_java_class,
};

define_java_class!(JavaCRC32, "dev.sanmer.zip.CRC32$Native");

pub extern "system" fn crc32<'env>(
    env: &'env JNIEnv,
    _cls: TrampolineClass<'env, JavaCRC32>,
    start: u32,
    bytes: TrampolineObject<'env, Array<i8>>,
    offset: i32,
    length: u32,
) -> u32 {
    let mut buf = Box::<[u8]>::new_uninit_slice(length as usize);
    typed_call!(env, env.typed_get_bytes_array_region(&bytes, offset, &mut buf));
    let buf = unsafe { buf.assume_init() };
    zlib_rs::crc32::crc32(start, &buf)
}
