$(document).ready(function() {
    const currentPath = window.location.pathname;

    // ホーム画面（/ または /home）の場合
    if (currentPath === '/' || currentPath === '/home') {
        // ナビゲーションを常に開いておく
        $('#headerNavCollapse').addClass('show');
    } else {
        // ホーム画面以外の場合は、「メニュー」ボタンを表示する
        $('.header-toggle-button').show();
    }
});