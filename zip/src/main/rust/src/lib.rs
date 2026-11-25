use crate::crc32::JavaCRC32;
use crate::inflater::JavaInflater;
use typed_jni::core::sys::{JNI_VERSION_1_6, jint};
use typed_jni::core::{JavaVM, NativeFunction};
use typed_jni::{LocalObject, TypedClassExt, TypedRef, TypedThrowableExt};

pub mod crc32;
pub mod inflater;

#[macro_export]
macro_rules! typed_call {
    ($env:expr, $ret:expr) => {
        match $ret {
            Ok(r) => r,
            Err(e) => {
                $env.typed_throw(&e);
                return Default::default();
            }
        }
    };
}

macro_rules! register_natives {
    ($env:expr, $cls:ty { $($fn_ptr:path = ($name:expr, $signature:expr)), *$(,)? }) => {{
        let cls = typed_call!($env, $env.typed_find_class::<$cls>());
        let ret = unsafe {
            $env.register_natives(
                &*cls,
                [$(NativeFunction { name: $name, signature: $signature, fn_ptr: $fn_ptr as _ }),*]
            ).map_err(|err| LocalObject::from_ref(err))
        };
        typed_call!($env, ret);
    }};
}

#[allow(non_snake_case)]
#[unsafe(no_mangle)]
pub extern "system" fn JNI_OnLoad(vm: &JavaVM, _: *const ()) -> jint {
    vm.with_current_env(|env| {
        register_natives!(
            env, JavaCRC32 {
                crc32::crc32 = (c"crc32", c"(I[BII)I"),
            }
        );
        register_natives!(
            env, JavaInflater {
                inflater::avail_in = (c"getAvailIn", c"()I"),
                inflater::avail_out = (c"getAvailOut", c"()I"),
                inflater::total_in = (c"getTotalIn", c"()J"),
                inflater::total_out = (c"getTotalOut", c"()J"),
                inflater::data_type = (c"getDataType", c"()I"),
                inflater::init = (c"init", c"(I)I"),
                inflater::prime = (c"prime", c"(II)I"),
                inflater::set_dictionary = (c"setDictionary", c"([BII)I"),
                inflater::inflate = (c"inflate", c"([BII[BIII)I"),
            }
        );
    });
    JNI_VERSION_1_6
}
