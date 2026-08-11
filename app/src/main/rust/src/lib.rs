use jni::objects::{JClass, JString};
use jni::sys::{jint, jstring};
use jni::JNIEnv;
use repak::PakBuilder;
use serde_json::json;
use std::fs::File;
use std::io::{Seek, SeekFrom, Write};
use std::os::fd::{FromRawFd, RawFd};
use std::sync::Mutex;

struct Session { file: File, pak: repak::PakReader }
static SESSION: Mutex<Option<Session>> = Mutex::new(None);

fn js(env: &mut JNIEnv, value: String) -> jstring {
    env.new_string(value).map(|s| s.into_raw()).unwrap_or(std::ptr::null_mut())
}
fn take_fd(fd: RawFd) -> File { unsafe { File::from_raw_fd(fd) } }

fn aes_key_from_hex(s: &str) -> Result<aes::Aes256, String> {
    let s = s.strip_prefix("0x").unwrap_or(s);
    let bytes = hex::decode(s).map_err(|e| format!("Invalid AES key: {e}"))?;
    if bytes.len() != 32 { return Err("AES key must be exactly 32 bytes (64 hex characters).".into()); }
    use aes::cipher::KeyInit;
    Ok(aes::Aes256::new_from_slice(&bytes).map_err(|e| e.to_string())?)
}

#[no_mangle]
pub extern "system" fn Java_com_example_uepakexplorer_NativePak_openPak(
    mut env: JNIEnv, _class: JClass, fd: jint, aes_key: JString
) -> jstring {
    let key_text: String = env.get_string(&aes_key).map(|s| s.into()).unwrap_or_default();
    let mut file = take_fd(fd);
    let builder = if key_text.trim().is_empty() {
        PakBuilder::new()
    } else {
        match aes_key_from_hex(key_text.trim()) {
            Ok(key) => PakBuilder::new().key(key),
            Err(e) => return js(&mut env, json!({"ok":false,"error":e}).to_string()),
        }
    };

    match builder.reader(&mut file) {
        Ok(pak) => {
            let files = pak.files();
            let compression = pak.used_compression().into_iter().map(|c| c.to_string()).collect::<Vec<_>>().join(", ");
            let value = json!({
                "ok": true,
                "version": pak.version().to_string(),
                "mountPoint": pak.mount_point(),
                "encryptedIndex": pak.encrypted_index(),
                "encryptionGuid": pak.encryption_guid().map(|v| format!("{v:032x}")),
                "fileCount": files.len(),
                "compression": if compression.is_empty() { "None" } else { &compression },
                "files": files
            });
            *SESSION.lock().unwrap() = Some(Session { file, pak });
            js(&mut env, value.to_string())
        }
        Err(e) => js(&mut env, json!({"ok":false,"error":e.to_string(),"encrypted":true}).to_string())
    }
}

#[no_mangle]
pub extern "system" fn Java_com_example_uepakexplorer_NativePak_closePak(_env: JNIEnv, _class: JClass) {
    *SESSION.lock().unwrap() = None;
}
#[no_mangle]
pub extern "system" fn Java_com_example_uepakexplorer_NativePak_search(
   mut env: JNIEnv, _class: JClass, query: JString, extension: JString
) -> jstring {
    let q: String = env.get_string(&query).map(|s| s.to_string_lossy().into_owned()).unwrap_or_default().to_lowercase();
    let ext: String = env.get_string(&extension).map(|s| s.into()).unwrap_or_default().to_lowercase();
    let guard = SESSION.lock().unwrap();
    let Some(s) = guard.as_ref() else { return js(&mut env, "[]".into()); };
    let results: Vec<&str> = s.pak.files().iter().filter(|p| {
        let pl = p.to_lowercase();
        let name_ok = q.is_empty() || pl.contains(&q);
        let ext_ok = ext.is_empty() || pl.ends_with(&ext);
        name_ok && ext_ok
    }).map(|p| p.as_str()).collect();
    js(&mut env, serde_json::to_string(&results).unwrap_or_else(|_| "[]".into()))
}

#[no_mangle]
pub extern "system" fn Java_com_example_uepakexplorer_NativePak_extract(
    mut env: JNIEnv, _class: JClass, path: JString, output_fd: jint
) -> jstring {
    let path: String = match env.get_string(&path) { Ok(s) => s.into(), Err(e) => return js(&mut env, e.to_string()) };
    let mut output = take_fd(output_fd);
    let mut guard = SESSION.lock().unwrap();
    let Some(s) = guard.as_mut() else { return js(&mut env, "No PAK opened".into()); };
    let _ = output.seek(SeekFrom::Start(0));
    match s.pak.read_file(&path, &mut s.file, &mut output) {
        Ok(()) => { let _ = output.flush(); js(&mut env, format!("Extracted: {path}")) }
        Err(e) => js(&mut env, format!("Extraction failed: {e}"))
    }
}
