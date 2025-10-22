// mypage_list.js

// ★★★ 修正: グローバル変数にHTMLから渡された初期データを直接格納 ★★★
let allUsersData = initialAllUsersData || []; 

// ページロード時の処理
$(document).ready(function() {
    // ユーザーデータをロード (API呼び出しではなく、初期データを直接使う)
    if (allUsersData.length > 0) {
        renderUserList(allUsersData);
    } else {
        // データがコントローラーから渡されなかった場合は、明示的にAPIを呼び出す
        // ただし、このパスを通る場合は通常エラーなので、ここでは何もしないか、ロード失敗を表示します。
        $('#userListAccordion').html('<p class="text-danger text-center">メンバー情報が見つかりませんでした。</p>');
    }
    
    // 検索イベントのハンドラを設定
    $('#searchButton').on('click', handleSearch);
    $('#userSearchInput').on('keypress', function(e) {
        if (e.which === 13) {
            handleSearch();
        }
    });
});

/**
 * APIから全ユーザー情報を取得し、グローバル変数に格納する
 * (WebSocketなしで更新する場合は、この関数はページに入るたびに実行される)
 *
 * 【注意】この関数は今回は使いませんが、互換性のため残しておきます。
 * 現在はコントローラーがデータを直接HTMLに埋め込んでいます。
 */
// function fetchAllUsers() { /* API呼び出しロジックは削除 */ }


/**
 * 検索キーワードに基づいてユーザーリストをフィルタリングし、描画する
 */
function handleSearch() {
    const query = $('#userSearchInput').val().toLowerCase().trim();
    
    // allUsersData (HTMLからロード済み) に対してフィルタリングを実行
    const filteredUsers = allUsersData.filter(user => {
        // 名前、メールアドレス、グループ、ステータスメッセージで検索
        const nameMatch = user.userName && user.userName.toLowerCase().includes(query);
        const emailMatch = user.mailAddress && user.mailAddress.toLowerCase().includes(query);
        const groupMatch = user.group && user.group.toLowerCase().includes(query);
        const statusMatch = user.statusMessage && user.statusMessage.toLowerCase().includes(query);
        
        return nameMatch || emailMatch || groupMatch || statusMatch;
    });

    renderUserList(filteredUsers);
}


/**
 * ユーザーのリストを元にアコーディオンUIを描画する
 * @param {Array} users - 表示するユーザーオブジェクトの配列
 */
function renderUserList(users) {
    const $accordion = $('#userListAccordion');
    $accordion.empty();
    
    if (users.length === 0) {
        const query = $('#userSearchInput').val();
        $accordion.html(`<p class="text-center text-muted">"${query}" に一致するメンバーはいません。</p>`);
        return;
    }

    users.forEach((user, index) => {
        const userId = user.userId;
        const targetId = `collapse-${userId}`;
        const headerId = `heading-${userId}`;
        const iconUrl = user.icon || '/images/default_icon.png';
        
        // マイページ登録項目が null の場合に '未設定' を表示するヘルパー関数
        const safeValue = (value) => value || '未設定';

        // ユーザー詳細情報をリストとして表示
        const detailList = `
            <ul>
                <li><strong>メールアドレス:</strong> ${safeValue(user.mailAddress)}</li>
                <li><strong>グループ/部署:</strong> ${safeValue(user.group)}</li>
                <li><strong>ステータスメッセージ:</strong> ${safeValue(user.statusMessage)}</li>
                <hr class="my-2">
                <li><strong>趣味:</strong> ${safeValue(user.hobby)}</li>
                <li><strong>マイブーム:</strong> ${safeValue(user.myBoom)}</li>
                <hr class="my-2">
                <li><strong>配属先:</strong> ${safeValue(user.deploymentDestination)}</li>
                <li><strong>勤務地:</strong> ${safeValue(user.deploymentArea)}</li>
                <li><strong>出社頻度:</strong> ${safeValue(user.commuteFrequency)}</li>
                <li><strong>作業時間:</strong> ${safeValue(user.workTime)}</li>
                <li><strong>業務内容:</strong> ${safeValue(user.workContent)}</li>
            </ul>
        `;

        const accordionItem = `
            <div class="accordion-item">
                <h2 class="accordion-header" id="${headerId}">
                    <button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#${targetId}" aria-expanded="false" aria-controls="${targetId}">
                        <img src="${iconUrl}" alt="${user.userName} Icon" class="user-icon-sm">
                        ${user.userName} <span class="badge bg-secondary ms-3">${safeValue(user.group)}</span>
                    </button>
                </h2>
                <div id="${targetId}" class="accordion-collapse collapse" aria-labelledby="${headerId}" data-bs-parent="#userListAccordion">
                    <div class="accordion-body">
                        ${detailList}
                    </div>
                </div>
            </div>
        `;
        $accordion.append(accordionItem);
    });
}