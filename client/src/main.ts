window.onload = () => {
    const w = window as any
    // Load Monaco code
    w.require(['vs/editor/editor.main'], () => {
        // Load client code
        require('./client');
    })
}
