import React, { useEffect, useRef, useState } from 'react';
import { database } from '../components/Firebase/firebaseConfig';
import { ref, onValue, push, set } from 'firebase/database';

const Chat = () => {
  const [users, setUsers] = useState([]);
  const [selectedUser, setSelectedUser] = useState(null);
  const [messages, setMessages] = useState([]);
  const [newMessage, setNewMessage] = useState('');
  const [unreadUsers, setUnreadUsers] = useState({});
  const messagesEndRef = useRef(null);

  // Fetch users
  useEffect(() => {
    const usersRef = ref(database, 'Users');
    const unsubscribe = onValue(usersRef, (snapshot) => {
      const usersData = snapshot.val();
      if (usersData) {
        const usersList = Object.keys(usersData).map(key => ({
          id: key,
          ...usersData[key]
        }));
        setUsers(usersList);
      }
    });

    return () => unsubscribe();
  }, []);

  // Fetch unread state for users
  useEffect(() => {
    const usersRef = ref(database, 'Users');
    const unsubscribe = onValue(usersRef, (snapshot) => {
      const data = snapshot.val();
      if (data) {
        const newUnread = {};
        for (let key in data) {
          const chats = data[key]?.chats;
          if (chats) {
            const lastMsgKey = Object.keys(chats).pop();
            const lastMsg = chats[lastMsgKey];
            if (lastMsg?.senderId === '1') {
              newUnread[key] = true;
            }
          }
        }
        setUnreadUsers(newUnread);
      }
    });

    return () => unsubscribe();
  }, []);

  // Fetch messages
  useEffect(() => {
    if (selectedUser) {
      const messagesRef = ref(database, `Users/${selectedUser.id}/chats`);
      const unsubscribe = onValue(messagesRef, (snapshot) => {
        const messagesData = snapshot.val();
        if (messagesData) {
          const messagesList = Object.keys(messagesData).map(key => ({
            id: key,
            ...messagesData[key]
          }));
          setMessages(messagesList);
        } else {
          setMessages([]);
        }

        // Đánh dấu là đã đọc
        setUnreadUsers(prev => {
          const updated = { ...prev };
          delete updated[selectedUser.id];
          return updated;
        });
      });

      return () => unsubscribe();
    }
  }, [selectedUser]);

  // Auto-scroll
  useEffect(() => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages]);

  const handleSendMessage = () => {
    if (newMessage.trim() === '' || !selectedUser) return;

    const messagesRef = ref(database, `Users/${selectedUser.id}/chats`);
    const newMessageRef = push(messagesRef);

    set(newMessageRef, {
      messageText: newMessage,
      senderId: '2',
      senderName: 'SHOP',
      timestamp: Date.now()
    });

    setNewMessage('');
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter') {
      handleSendMessage();
    }
  };

  return (
    <div className="chat-container">
      <div className="user-list">
        <h2>Users</h2>
        <ul>
          {users.map(user => (
            <li
              key={user.id}
              className={selectedUser?.id === user.id ? 'active' : ''}
              onClick={() => setSelectedUser(user)}
            >
              <div className="user-info">
                <span className="user-name">
                  {user.profile_name}
                  {unreadUsers[user.id] && <span className="unread-dot" />}
                </span>
              </div>
            </li>
          ))}
        </ul>
      </div>

      <div className="chat-area">
        {selectedUser ? (
          <>
            <div className="chat-header">
              <h2>Chat with {selectedUser.auth_email}</h2>
            </div>

            <div className="messages">
              {messages.length > 0 ? (
                messages.map(message => (
                  <div
                    key={message.id}
                    className={`message ${message.senderId === '1' ? 'left' : 'right'}`}
                  >
                    <div className="message-content">
                      <span className="message-sender">{message.senderName}</span>
                      <p>{message.messageText}</p>
                    </div>
                  </div>
                ))
              ) : (
                <div className="no-messages">
                  <p>No messages yet. Start the conversation!</p>
                </div>
              )}
              <div ref={messagesEndRef} />
            </div>

            <div className="message-input">
              <input
                type="text"
                value={newMessage}
                onChange={(e) => setNewMessage(e.target.value)}
                onKeyPress={handleKeyPress}
                placeholder="Type a message..."
              />
              <button onClick={handleSendMessage}>Send</button>
            </div>
          </>
        ) : (
          <div className="no-user-selected">
            <p>Select a user to start chatting</p>
          </div>
        )}
      </div>

      <style jsx>{`
        .chat-container {
          display: flex;
          height: 100vh;
        }

        .user-list {
          width: 300px;
          background-color: #f5f5f5;
          border-right: 1px solid #ddd;
          overflow-y: auto;
        }

        .user-list h2 {
          padding: 15px;
          margin: 0;
          background-color: #2c3e50;
          color: white;
        }

        .user-list ul {
          list-style: none;
          padding: 0;
          margin: 0;
        }

        .user-list li {
          padding: 15px;
          border-bottom: 1px solid #ddd;
          cursor: pointer;
        }

        .user-list li:hover {
          background-color: #e9e9e9;
        }

        .user-list li.active {
          background-color: #3498db;
          color: white;
        }

        .user-info {
          display: flex;
          align-items: center;
          justify-content: space-between;
        }

        .user-name {
          font-weight: bold;
          position: relative;
        }

        .unread-dot {
          display: inline-block;
          width: 8px;
          height: 8px;
          background-color: red;
          border-radius: 50%;
          margin-left: 8px;
        }

        .chat-area {
          flex: 1;
          display: flex;
          flex-direction: column;
        }

        .chat-header {
          padding: 15px;
          background-color: #2c3e50;
          color: white;
        }

        .messages {
          flex: 1;
          padding: 20px;
          overflow-y: auto;
          background-color: #e5ddd5;
          display: flex;
          flex-direction: column;
        }

        .message {
          margin-bottom: 15px;
          max-width: 70%;
        }

        .message-content {
          padding: 10px 15px;
          border-radius: 18px;
          position: relative;
        }

        .message.left {
          margin-right: auto;
        }

        .message.right {
          margin-left: auto;
        }

        .message.left .message-content {
          background-color: white;
        }

        .message.right .message-content {
          background-color: #dcf8c6;
        }

        .message-sender {
          font-size: 0.75em;
          font-weight: bold;
          margin-bottom: 4px;
          display: block;
        }

        .message-input {
          padding: 15px;
          background-color: #f5f5f5;
          display: flex;
        }

        .message-input input {
          flex: 1;
          padding: 10px;
          border: 1px solid #ddd;
          border-radius: 20px;
          outline: none;
        }

        .message-input button {
          margin-left: 10px;
          padding: 10px 20px;
          background-color: #2c3e50;
          color: white;
          border: none;
          border-radius: 20px;
          cursor: pointer;
        }

        .no-user-selected {
          flex: 1;
          display: flex;
          justify-content: center;
          align-items: center;
          background-color: #e5ddd5;
        }

        .no-messages {
          flex: 1;
          display: flex;
          justify-content: center;
          align-items: center;
          color: #666;
        }
      `}</style>
    </div>
  );
};

export default Chat;
