use tauri::Builder;
fn main() {
    let builder = Builder::default();
    let _ = builder.on_navigation(|_app, _url| true);
}
