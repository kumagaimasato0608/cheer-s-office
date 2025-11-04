// --- グローバル変数・定数 ---
let me = { id: "", name: "" };
let users = [];
let rooms = [];
let currentRoom = null;
let stomp = null;
let sub = null;
let selectedFile = null;
let notificationCounts = {};

const DEFAULT_ICON = "/images/default_icon.png";
const DEFAULT_GROUP_ICON = "/images/groups/default-group-icon.png";

// --- ユーティリティ関数 ---
function jstTime(iso) { try { return new Date(iso).toLocaleTimeString("ja-JP", { hour: "2-digit", minute: "2-digit" }); } catch (e) { return ""; }};
function scrollToBottom() { const box = $("#messages")[0]; if (box) box.scrollTop = box.scrollHeight; };
function getOtherId(room) { return (room.members || []).find(id => id !== me.id); }
function findUser(id) { return users.find(u => u.userId === id); }
function getOtherUser(room) { return findUser(getOtherId(room)); }
function getOtherUserName(room) { return (getOtherUser(room)?.userName) || "チャット"; }
function getOtherUserIcon(room) { return (getOtherUser(room)?.icon) || DEFAULT_ICON; }
function escapeHtml(str) { return String(str).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;"); }

/**
 * ★★★ URLをリンクに変換する関数 (ここが追加されました) ★★★
 * テキスト内のURLを検出し、クリック可能なリンクに変換する関数
 */
function linkify(text) {
  // http:// または https://... で始まるURL
  const urlPattern = /(\b(https?|ftp|file):\/\/[-A-Z0-9+&@#\/%?=~_|!:,.;]*[-A-Z0-9+&@#\/%=~_|])/ig;
  // www. から始まるURL
  const pseudoUrlPattern = /(^|[^\/])(www\.[\S]+(\b|$))/gim;
  
  let linkedText = text.replace(urlPattern, '<a href="$1" target="_blank" rel="noopener noreferrer">$1</a>');
  linkedText = linkedText.replace(pseudoUrlPattern, '$1<a href="http://$2" target="_blank" rel="noopener noreferrer">$2</a>');
  return linkedText;
}


// --- 画面描画（レンダー）関数 ---
function renderRooms(roomsToRender) {
    const displayRooms = roomsToRender || rooms;
    const $list = $("#roomList").empty();
    if (!displayRooms.length) {
        const keyword = $('#roomSearchInput').val().trim();
        const message = keyword ? "該当するチャットはありません" : "まだチャットがありません";
        $list.append(`<div style="text-align:center; color:#777; padding:20px 0;">${message}</div>`);
        return;
    }
    displayRooms.forEach(r => {
        const active = (currentRoom && currentRoom.roomId === r.roomId) ? "active" : "";
        const isGroupChat = (r.roomName && (r.members || []).length > 2);
        const name = isGroupChat ? r.roomName : getOtherUserName(r);
        const icon = isGroupChat ? (r.icon || DEFAULT_GROUP_ICON) : getOtherUserIcon(r);
        const count = notificationCounts[r.roomId] || 0;

        $list.append(
            `<div class="room-item ${active}" data-room-id="${r.roomId}">
                <img src="${icon}" class="room-icon" />
                <span class="room-name">${escapeHtml(name)}</span>
                <span class="notification-badge ${count > 0 ? 'visible' : ''}">${count}</span>
            </div>`
        );
    });
}

function renderHeader() {
  if (!currentRoom) {
    $("#headerTitle").text("ルーム未選択");
    return;
  }
  const isGroupChat = currentRoom.roomName && (currentRoom.members || []).length > 2;
  let title = "";
  if (isGroupChat) {
    const memberNames = (currentRoom.members || [])
      .map(id => {
        const user = (id === me.id) ? me : findUser(id);
        return user ? (user.name || user.userName) : null;
      })
      .filter(name => name);

    title = `${escapeHtml(currentRoom.roomName)}（${memberNames.join('、')}）`;
  } else {
    title = getOtherUserName(currentRoom);
  }
  $("#headerTitle").text(title);
}

function renderMessages(msgs) {
  const $msgs = $("#messages").empty();
  let lastDate = ""; // 直前のメッセージ日付を保持

  (msgs || []).forEach(m => {
    const msgDate = new Date(m.timestamp).toLocaleDateString("ja-JP", {
      year: "numeric", month: "2-digit", day: "2-digit"
    });

    // 日付が変わったら区切りを挿入
    if (msgDate !== lastDate) {
      $msgs.append(`<div class="date-separator">${msgDate}</div>`);
      lastDate = msgDate;
    }

    const isMine = (m.userId === me.id);
    const wrapperClass = isMine ? "me" : "other";
    const user = findUser(m.userId) || { userName: m.userName, icon: m.icon };

    let readStatusHtml = '';
    if (isMine && m.readBy && currentRoom) {
      const otherReadersCount = (m.readBy || []).filter(id => id !== me.id).length;

      if ((currentRoom.members || []).length > 2 && otherReadersCount > 0) {
        readStatusHtml = `<span class="msg-read-status">既読 ${otherReadersCount}</span>`;
      } else if ((currentRoom.members || []).length === 2 && otherReadersCount > 0) {
        readStatusHtml = `<span class="msg-read-status">既読</span>`;
      }
    }

    let messageBodyHtml = '';
    if (m.type === 'IMAGE') {
      messageBodyHtml = `<img src="${m.content}" alt="添付画像" class="chat-image-standalone" onclick="window.open('${m.content}')">`;
      if (m.caption) messageBodyHtml += `<div class="msg">${escapeHtml(m.caption)}</div>`;
    } else {
      // ★★★ ここが修正されました ★★★
      // 1. まずHTMLエスケープを行う（安全のため）
      const safeText = escapeHtml(m.content || "");
      // 2. 安全なテキストからURLを探してリンク化する
      const linkedText = linkify(safeText);
      messageBodyHtml = `<div class="msg">${linkedText}</div>`;
    }

    const html = `
    <div class="msg-wrapper ${wrapperClass}" data-user-id="${m.userId}" data-message-id="${m.messageId}">
      ${!isMine ? `<img src="${user.icon || DEFAULT_ICON}" class="msg-icon" />` : "" }
      <div class="msg-content-wrapper">
        <div class="msg-meta">${escapeHtml(user.userName || "不明")}　${jstTime(m.timestamp)}</div>
        <div class="msg-body">
          ${readStatusHtml}
          <div class="message-container">${messageBodyHtml}</div>
        </div>
      </div>
    </div>`;
    $msgs.append(html);
  });

  scrollToBottom();
}

// --- WebSocket・通信関連 ---
function subscribeRoom(roomId) {
  if (!stomp || !stomp.connected) return;
  if (sub) sub.unsubscribe();
  sub = stomp.subscribe(`/topic/chat/${roomId}`, msg => {
    const payload = JSON.parse(msg.body);

    // --- 既読通知の処理 ---
    if (payload.type === 'READ_UPDATE') {
      const { messageId, readBy } = payload;
      const $msgWrapper = $(`.msg-wrapper[data-message-id="${messageId}"]`);

      if ($msgWrapper.hasClass('me')) {
        const otherReadersCount = (readBy || []).filter(id => id !== me.id).length;
        let statusText = '';

        if ((currentRoom.members || []).length > 2 && otherReadersCount > 0) {
          statusText = `既読 ${otherReadersCount}`;
        } else if ((currentRoom.members || []).length === 2 && otherReadersCount > 0) {
          statusText = '既読';
        }

        if (statusText) {
          const $statusSpan = $msgWrapper.find('.msg-read-status');
          if ($statusSpan.length > 0) $statusSpan.text(statusText);
          else $msgWrapper.find('.msg-body').prepend(`<span class="msg-read-status">${statusText}</span>`);
        }
      }
      return;
    }

    // --- 新規メッセージの処理 ---
    const isMine = (payload.userId === me.id);
    const user = findUser(payload.userId) || { userName: payload.userName, icon: payload.icon };
    let messageBodyHtml = '';
    if (payload.type === 'IMAGE') {
      messageBodyHtml = `<img src="${payload.content}" alt="添付画像" class="chat-image-standalone" onclick="window.open('${payload.content}')">`;
      if (payload.caption) messageBodyHtml += `<div class="msg">${escapeHtml(payload.caption)}</div>`;
    } else {
      // ★★★ ここが修正されました ★★★
      // 1. まずHTMLエスケープを行う（安全のため）
      const safeText = escapeHtml(payload.content || "");
      // 2. 安全なテキストからURLを探してリンク化する
      const linkedText = linkify(safeText);
      messageBodyHtml = `<div class="msg">${linkedText}</div>`;
    }

    const html = `
    <div class="msg-wrapper ${isMine ? 'me' : 'other'}" data-user-id="${payload.userId}" data-message-id="${payload.messageId}">
      ${!isMine ? `<img src="${user.icon || DEFAULT_ICON}" class="msg-icon" />` : "" }
      <div class="msg-content-wrapper">
        <div class="msg-meta">${escapeHtml(user.userName || "不明")}　${jstTime(payload.timestamp)}</div>
        <div class="msg-body">
          <div class="message-container">
            ${messageBodyHtml}
          </div>
        </div>
      </div>
    </div>`;
    $("#messages").append(html);
    scrollToBottom();

    if (!isMine && document.visibilityState === 'visible') {
      stomp.send(`/app/chat/${roomId}/read`, {}, JSON.stringify({ messageId: payload.messageId, userId: me.id }));
    }
  });
}

function loadMe() { return $.getJSON("/api/users/me").then(res => { me.id = res.userId; me.name = res.userName; }); }
function loadUsers() { return $.getJSON("/api/users").then(res => { users = (res || []).filter(u => u.userId !== me.id); }); }
function loadMessages(roomId) { return $.getJSON(`/api/chat/${roomId}`); }

function connectWS(cb) {
  if (stomp && stomp.connected) {
    if (cb) cb();
    return;
  }
  const sock = new SockJS("/ws");
  stomp = Stomp.over(sock);
  stomp.debug = null;
  stomp.connect({}, () => {
    if (me.id) {
      stomp.subscribe(`/topic/notifications/${me.id}`, msg => {
        const notification = JSON.parse(msg.body);
        if (notification.type === 'NEW_MESSAGE') {
          if (!currentRoom || currentRoom.roomId !== notification.roomId) {
            notificationCounts[notification.roomId] = (notificationCounts[notification.roomId] || 0) + 1;
            renderRooms();
          }
        }
      });
    }
    if (typeof cb === "function") cb();
  });
}

// --- メインの操作ロジック ---

// ▼▼▼ ★★★ ここは元の修正（既読処理）が反映されたままです ★★★ ▼▼▼
function selectRoomById(roomId) {
  const room = rooms.find(r => r.roomId === roomId);
  if(!room) return;

  // 1. バッジを先に消す
  notificationCounts[roomId] = 0;
  currentRoom = room;
  renderRooms();
  renderHeader();
  $("#inputArea").css("display", "flex");
  $("#sendBtn").prop("disabled", false);

  // 2. メッセージをサーバーから読み込む
  loadMessages(roomId).then(loadedMessages => {

    // ★ 修正: 既読にするメッセージIDをすべて保存する配列
    const unreadMessageIds = [];

    // 3. 【自分用の修正】
    // 画面に表示する *前* に、未読メッセージをローカルデータ上で既読にする
    (loadedMessages || []).forEach(msg => {
        if (msg.userId !== me.id && !(msg.readBy || []).includes(me.id)) {
            if (!msg.readBy) {
                msg.readBy = [];
            }
            msg.readBy.push(me.id); // ローカルの 'readBy' リストに自分を追加

            // ★ 修正: 最後の1件だけでなく、すべての未読IDを保存
            unreadMessageIds.push(msg.messageId);
        }
    });

    // 4. 既読に書き換えたデータで画面を描画する (自分の画面が正しくなる)
    renderMessages(loadedMessages);

    // 5. WebSocketに接続
    connectWS(() => {
      subscribeRoom(roomId);

      // 6. ★ 修正: unreadMessageIds配列にIDが1件でもあれば
      if (unreadMessageIds.length > 0) {

          // 6a. 【サーバー用の修正】
          // バックエンドのJSONファイルを *1回だけ* 更新
          stomp.send(`/app/chat/${roomId}/markAllAsRead`, {}, JSON.stringify({ userId: me.id }));

          // 6b. 【相手用の修正】
          // 相手（送信者）に「既読」を伝えるため、
          // 既読にした *すべてのメッセージ* について 'read' リクエストを送信する
          unreadMessageIds.forEach(msgId => {
             stomp.send(`/app/chat/${roomId}/read`, {}, JSON.stringify({
                  messageId: msgId,
                  userId: me.id
              }));
          });
      }
    });
  });
}
// ▲▲▲ ★★★ 既読処理の修正箇所はここまで ★★★ ▲▲▲

function sendMessage() {
  if(!currentRoom || !stomp || !stomp.connected) return;
  const text = $("#messageInput").val().trim();
  if(!text && !selectedFile) return;
  if(selectedFile) uploadAndSendMessage(selectedFile, text);
  else sendTextMessage(text);
}

function sendTextMessage(text) {
  const meUser = findUser(me.id) || me;
  const payload = { roomId: currentRoom.roomId, userId: me.id, userName: me.name, type: "TEXT", content: text, timestamp: new Date().toISOString(), icon: meUser.icon || DEFAULT_ICON };
  stomp.send(`/app/chat/${payload.roomId}`, {}, JSON.stringify(payload));
  $("#messageInput").val("").css("height", "44px");
}

function uploadAndSendMessage(file, caption) {
  const formData = new FormData();
  formData.append("file", file);
  $("#sendBtn, #attachFileBtn").prop("disabled", true);
  $.ajax({ url: "/api/chat/upload", method: "POST", data: formData, processData: false, contentType: false })
    .done(res => {
      if(res && res.imageUrl) {
        const meUser = findUser(me.id) || me;
        const payload = { roomId: currentRoom.roomId, userId: me.id, userName: me.name, type: "IMAGE", content: res.imageUrl, caption: caption, timestamp: new Date().toISOString(), icon: meUser.icon || DEFAULT_ICON };
        stomp.send(`/app/chat/${payload.roomId}`, {}, JSON.stringify(payload));
        clearPreview();
        $("#messageInput").val("").css("height", "44px");
      }
    }).fail(() => alert("画像アップロード失敗"))
    .always(() => $("#sendBtn, #attachFileBtn").prop("disabled", false));
}

function setupPreview(file) {
  if(!file.type.startsWith("image/")) { alert("画像ファイルを選択してください"); return; }
  selectedFile = file;
  const reader = new FileReader();
  reader.onload = e => $("#previewArea").html(
    `<div class="preview-container">
      <img src="${e.target.result}" class="preview-image" />
      <button class="preview-remove">×</button>
    </div>`).show();
  reader.readAsDataURL(file);
}
function clearPreview() {
  selectedFile = null;
  $("#fileInput").val("");
  $("#previewArea").empty().hide();
}

function openCreateModal() {
  const $ul = $("#userList").empty();
  users.forEach(u => $ul.append(
    `<div class="user-row">
      <img src="${u.icon || DEFAULT_ICON}" class="user-icon" />
      <label><input type="checkbox" name="targetUser" value="${u.userId}" />${escapeHtml(u.userName)}</label>
    </div>`
  ));
  $("#createModal").css("display", "flex");
}

function createRoom() {
  const selectedUsers = $('input[name="targetUser"]:checked').map(function() { return $(this).val(); }).get();
  if (selectedUsers.length === 0) { alert("相手を1人以上選択してください。"); return; }
  if (selectedUsers.length === 1) {
    createSingleChat(selectedUsers[0]);
  } else {
    window.selectedGroupMembers = [me.id, ...selectedUsers];
    const $memberList = $("#selectedMemberList").empty();
    window.selectedGroupMembers.forEach(id => {
      const user = (id === me.id) ? me : findUser(id);
      if (user) {
        const userName = (id === me.id) ? user.name : user.userName;
        const userIcon = user.icon || DEFAULT_ICON;
        $memberList.append(
          `<div class="member-item">
            <img src="${userIcon}" class="member-icon" />
            <span class="member-name">${escapeHtml(userName)}</span>
          </div>`
        );
      }
    });
    $("#createModal").hide();
    $("#groupModal").css("display", "flex");
  }
}

function createSingleChat(targetId) {
  $.ajax({ url: "/api/rooms", method: "POST", contentType: "application/json", data: JSON.stringify({ members: [me.id, targetId] }) })
    .then(newRoom => {
      if (!rooms.some(r => r.roomId === newRoom.roomId)) { rooms.push(newRoom); }
      renderRooms();
      $("#createModal").hide();
      selectRoomById(newRoom.roomId);
    });
}

function openDeleteModal() {
  if(!currentRoom) { alert("削除するチャットを選択してください"); return; }
  $("#deleteModal").css("display", "flex");
}

function deleteRoomConfirm() {
  if(!currentRoom) return;
  const rid = currentRoom.roomId;
  $.ajax({ url: `/api/rooms/${rid}`, method: "DELETE" })
    .then(() => {
      rooms = rooms.filter(r => r.roomId !== rid);
      currentRoom = null;
      renderRooms();
      renderHeader();
      $("#messages").empty();
      $("#inputArea").hide();
      $("#deleteModal").hide();
    });
}

function uploadGroupIcon(file) {
  const formData = new FormData();
  formData.append("file", file);
  return $.ajax({ url: "/api/rooms/uploadIcon", method: "POST", data: formData, processData: false, contentType: false })
    .then(response => response.iconUrl);
}

function createGroupRoom(name, icon) {
  const payload = { members: window.selectedGroupMembers, roomName: name, icon: icon };
  $.ajax({ url: "/api/rooms", method: "POST", contentType: "application/json", data: JSON.stringify(payload) })
    .then(newRoom => {
      if (!rooms.some(r => r.roomId === newRoom.roomId)) { rooms.push(newRoom); }
      renderRooms();
      $("#groupModal").hide();
      resetGroupModal();
      selectRoomById(newRoom.roomId);
    });
}

function resetGroupModal() {
  $("#groupNameInput").val("");
  $("#groupIconInput").val("");
  $("#groupIconPreview").attr("src", DEFAULT_GROUP_ICON);
  $("#selectedMemberList").empty();
  window.selectedGroupMembers = [];
}

// --- ページの初期化・イベントリスナー設定 ---
$(async function init() {
    // ★★★ この CSRF 設定コードを追加 ★★★
    const token = $("meta[name='_csrf']").attr("content");
    const header = $("meta[name='_csrf_header']").attr("content");
    if (token && header) {
        $.ajaxSetup({
            beforeSend: function(xhr) {
                // console.log("★ beforeSend が実行されました ★ ヘッダー:", header, "トークン:", token); // デバッグ用
                xhr.setRequestHeader(header, token);
            }
        });
        // console.log("CSRF setup complete. Header:", header, "Token:", token); // デバッグ用
    } else {
        console.error("CSRF token meta tags not found!");
    }
    // ★★★ ここまで追加 ★★★
    
    // --- イベントリスナー設定 ---
    $(document).on("click", ".room-item", function() { selectRoomById($(this).data("room-id")); });
    $(document).on("click", ".preview-remove", clearPreview);
    $("#sendBtn").on("click", sendMessage);
    $("#messageInput").on("keydown", function(e) {
        if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); sendMessage(); }
    });
    $("#messageInput").on("input", function() {
        this.style.height = 'auto';
        this.style.height = (this.scrollHeight) + 'px';
    });
    $("#attachFileBtn").on("click", () => { if (!$("#attachFileBtn").prop("disabled")) $("#fileInput").click(); });
    $("#fileInput").on("change", function(e) { if (e.target.files.length > 0) setupPreview(e.target.files[0]); });

    // ドラッグ＆ドロップ
    const $chatMain = $(".chat-main");
    $chatMain.on({
        "dragover": (e) => { e.preventDefault(); e.stopPropagation(); $chatMain.addClass("drag-over"); },
        "dragleave": (e) => { e.preventDefault(); e.stopPropagation(); $chatMain.removeClass("drag-over"); },
        "drop": (e) => { e.preventDefault(); e.stopPropagation(); $chatMain.removeClass("drag-over"); if (e.originalEvent.dataTransfer.files.length > 0) setupPreview(e.originalEvent.dataTransfer.files[0]); }
    });

    // プロフィールモーダル
    $("#messages").on("click", ".msg-wrapper.other .msg-icon", function() {
        const userId = $(this).closest(".msg-wrapper").data("user-id");
        if (!userId) return;

        $.getJSON(`/api/users/${userId}`).done(fullUser => { // GETリクエストなのでCSRF不要
            if (fullUser) {
                $("#profileIcon").attr("src", fullUser.icon || DEFAULT_ICON);
                $("#profileUsername").text(fullUser.userName || "不明");
                $("#profileStatus").text(fullUser.statusMessage || "");
                $("#profileGroup").text(fullUser.group || "未設定");
                $("#profileHobby").text(fullUser.hobby || "未設定");
                $("#profileMyBoom").text(fullUser.myBoom || "未設定");
                $("#profileModal").css("display", "flex");
            }
        }).fail(() => {
            alert("ユーザー情報の取得に失敗しました。");
        });
    });
    $("#closeProfileBtn, #profileModal").on("click", function(e) {
        if (e.target === this || $(e.target).is("#closeProfileBtn")) {
            $("#profileModal").hide();
        }
    });
    $(".profile-modal-content").on("click", function(e) {
        e.stopPropagation();
    });

    // 新規・削除モーダル
    $("#openCreateBtn").on("click", openCreateModal);
    $("#cancelCreateBtn").on("click", () => $("#createModal").hide());
    $("#createRoomBtn").on("click", createRoom);
    $("#openDeleteBtn").on("click", openDeleteModal);
    $("#cancelDeleteBtn").on("click", () => $("#deleteModal").hide());
    $("#confirmDeleteBtn").on("click", deleteRoomConfirm);

    // グループ作成モーダル
    $("#createGroupBtn").on("click", function() {
        const groupName = $("#groupNameInput").val().trim();
        const iconFile = $("#groupIconInput")[0].files[0];
        if (!groupName) { alert("グループ名を入力してください。"); return; }
        if (iconFile) {
            uploadGroupIcon(iconFile).then(iconUrl => createGroupRoom(groupName, iconUrl))
                .catch(() => { alert("アイコンのアップロードに失敗しました。"); });
        } else {
            createGroupRoom(groupName, DEFAULT_GROUP_ICON);
        }
    });
    $("#cancelGroupBtn").on("click", function() { $("#groupModal").hide(); resetGroupModal(); });
    $("#groupIconInput").on("change", function(e) {
        if (e.target.files && e.target.files[0]) {
            const reader = new FileReader();
            reader.onload = e => $("#groupIconPreview").attr("src", e.target.result);
            reader.readAsDataURL(e.target.files[0]);
        }
    });

    // --- 初期データ読み込み ---
    try {
        await loadMe();
        await loadUsers();

        await $.getJSON("/api/rooms").then(res => { // GETリクエストなのでCSRF不要
            rooms = (res || []).filter(r => (r.members || []).includes(me.id));

            // 既読カウントの初期化
            notificationCounts = {};
            rooms.forEach(room => {
                if (room.unreadCount > 0) {
                    notificationCounts[room.roomId] = room.unreadCount;
                }
            });
        });

        renderRooms();
        connectWS(); // WebSocketに接続（通知購読のため）
    } catch (e) {
        console.error("初期化に失敗しました:", e);
        alert("初期化に失敗しました。リロードしてください。");
    }
});

/* === 🔍 チャット検索機能（掲示板と同じEnterキー確定） === */
function searchRooms() {
  const keyword = $("#roomSearchInput").val().toLowerCase().trim();
  let found = false;

  $(".room-item").each(function() {
    const roomName = $(this).find(".room-name").text().toLowerCase();
    const isVisible = roomName.includes(keyword);
    $(this).toggle(isVisible);
    if(isVisible) found = true;
  });

  // 検索結果が0件の場合のメッセージ表示
  $("#roomList .empty-hint").remove(); // 既存のメッセージを全て削除
  if (!found) {
    if ($("#roomList .room-item:visible").length === 0) { // 表示されているアイテムが本当に0か確認
      const message = keyword ? "該当するチャットはありません" : "まだチャットがありません";
      $("#roomList").append(`<div class="empty-hint" style="text-align:center; color:#777; padding:20px 0;">${message}</div>`);
    }
  }
}