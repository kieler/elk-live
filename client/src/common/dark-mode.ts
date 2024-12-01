import { setupDarkMode as setup } from "simple-dark-mode-toggle";

export function setupDarkMode() {
    const darkModeToggle = <HTMLElement>document.getElementById('darkModeToggle');
    setup(darkModeToggle, {
        darkClassName: 'elk-dark-mode',
        storageName: 'elk-live-dark-mode',
        darkByDefault: false,
    });
}
