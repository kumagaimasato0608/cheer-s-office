'use strict';

let stompClient = null;
let currentRoomId = null;
let currentUserId = null;

document.addEventListener('DOMContentLoaded', function() {
    const roomIdElement = document.getElementById('currentRoomId');
    const userIdElement = document.getElementById('currentUserId');

    if (roomIdElement && userIdElement) {
        currentRoomId = roomIdElement.value;
        currentUserId = userIdElement.value;
        console.log("Current Room ID:", currentRoomId);
        console.log("Current User ID:", currentUserId);
        connect();
    } else {
        console.error("Required elements (currentRoomId, currentUserId) not found in HTML.");
        const messageArea = document.getElementById('messageArea');
        if(messageArea) {
            messageArea.innerHTML = '<p style="color:red;">初期化エラー: ユーザー情報が見つかりません。リロードしてください。</p>';
        }
        return;
    }

    const messageForm = document.getElementById('messageForm');
    if (messageForm) {
        messageForm.addEventListener('submit', sendMessage);
    } else {
        console.error("Message form element (messageForm) not found.");
    }
});

function connect() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, onConnected, onError);
}

function onConnected() {
    stompClient.subscribe('/topic/chat/' + currentRoomId, onMessageReceived);
    console.log('WebSocket接続完了。ルームID: ' + currentRoomId);
}

function onError(error) {
    console.error('WebSocket接続に失敗しました: ' + error);
    const messageArea = document.getElementById('messageArea');
    if(messageArea) {
        messageArea.innerHTML = '<p style="color:red;">WebSocket接続エラー。リロードしてください。</p>';
    }
}

function sendMessage(event) {
    const messageInput = document.getElementById('messageInput');
    const messageContent = messageInput ? messageInput.value.trim() : '';

    if (messageContent && stompClient && currentRoomId && currentUserId) {
        const chatMessage = {
            roomId: currentRoomId,
            sender: currentUserId,
            content: messageContent
        };
        
        stompClient.send("/app/chat/" + currentRoomId, {}, JSON.stringify(chatMessage));
        if (messageInput) {
            messageInput.value = '';
        }
    } else {
        console.warn("メッセージが空、STOMPクライアント未接続、または情報不足のため送信できません。");
    }
    if (event) {
        event.preventDefault();
    }
}

function onMessageReceived(payload) {
    const message = JSON.parse(payload.body);
    
    const messageArea = document.getElementById('messageArea');
    if (!messageArea) {
        console.error("Message area element (messageArea) not found.");
        return;
    }

    const messageElement = document.createElement('div');
    const isMyMessage = message.sender === currentUserId;
    messageElement.classList.add('chat-message', isMyMessage ? 'my-message' : 'other-message');
    
    messageElement.innerHTML = `
        <span style="font-weight: bold;">[${message.sender}]</span>
        <span>${message.content}</span>
        <span style="font-size: 0.8em; color: #888;">(${new Date(message.timestamp).toLocaleTimeString('ja-JP')})</span>
    `;

    messageArea.appendChild(messageElement);
    messageArea.scrollTop = messageArea.scrollHeight;
}