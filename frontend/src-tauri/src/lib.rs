use tauri::Manager;

#[tauri::command]
async fn open_login_popup(app: tauri::AppHandle, url: String) -> Result<(), String> {
    if let Some(win) = app.get_webview_window("keycloak-login") {
        let _ = win.set_focus();
        return Ok(());
    }
    
    tauri::WebviewWindowBuilder::new(
        &app,
        "keycloak-login",
        tauri::WebviewUrl::External(url.parse().map_err(|e| format!("Invalid URL: {}", e))?)
    )
    .title("Autentificare Keycloak")
    .inner_size(500.0, 700.0)
    .center()
    .resizable(false)
    .on_navigation(|url| {
        println!("Popup attempting to navigate to: {}", url);
        true // Allow Keycloak OAuth redirects in the popup!
    })
    .build()
    .map_err(|e| e.to_string())?;
    
    Ok(())
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_shell::init())
        .setup(|app| {
            if cfg!(debug_assertions) {
                app.handle().plugin(
                    tauri_plugin_log::Builder::default()
                        .level(log::LevelFilter::Info)
                        .build(),
                )?;
            }
            tauri::WebviewWindowBuilder::new(
                app,
                "main",
                tauri::WebviewUrl::App("index.html".into())
            )
            .title("OphthaCloud EMR & POS")
            .inner_size(1280.0, 800.0)
            .on_navigation(|_url| {
                true // Allow Keycloak OAuth redirects to proceed within the webview
            })
            .build()?;
            Ok(())
        })
        .invoke_handler(tauri::generate_handler![open_login_popup])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
