import { initializeApp } from 'firebase/app';
import { getDatabase } from 'firebase/database';

const firebaseConfig = {
  apiKey: "AIzaSyChKKYg9z5W_9hYjhQg2rJqsJtzUnLI-WE",
  authDomain: "appgiadung-702d4.firebaseapp.com",
  databaseURL: "https://appgiadung-702d4-default-rtdb.firebaseio.com",
  projectId: "appgiadung-702d4",
  storageBucket: "appgiadung-702d4.firebasestorage.app",
  messagingSenderId: "83496255767",
  appId: "1:83496255767:web:c3082025c83422f402b752"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const database = getDatabase(app);

export { database };
