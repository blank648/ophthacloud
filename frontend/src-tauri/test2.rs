use tauri::Manager;
fn main() {
    tauri::Builder::default()
        .setup(|app| {
            let win = app.get_webview_window("main").unwrap();
            win.on_navigation(|url| true);
            Ok(())
        });
}
