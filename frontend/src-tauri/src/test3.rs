use tauri::WebviewWindowBuilder;
use tauri::WebviewUrl;

fn main() {
    let _builder = tauri::Builder::default()
        .setup(|app| {
            let _window = WebviewWindowBuilder::new(
                app,
                "main",
                WebviewUrl::App("index.html".into())
            )
            .on_navigation(|_url| true)
            .build()?;
            Ok(())
        });
}
