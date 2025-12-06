package com.example.app.Activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app.Adapter.ChatAdapter
import com.example.app.Helper.TinyDB
import com.example.app.Model.*
import com.example.app.Network.RetrofitClient
import com.example.app.Network.SendMessageRequest
import com.example.app.databinding.ActivityMyChatBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMyChatBinding
    private lateinit var tinyDB: TinyDB
    private lateinit var chatAdapter: ChatAdapter
    private val chatList = ArrayList<ChatMessage>()
    private var lastMessageId: Long = 0
    private val pollingHandler = Handler(Looper.getMainLooper())
    private var pollingRunnable: Runnable? = null
    private val POLLING_INTERVAL = 3000L // 3 giây
    private var currentUserId: Long = 0
    private var adminId: Int = 1 // Mặc định admin id = 1

    companion object {
        private const val TAG = "MyChatActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate started")
        binding = ActivityMyChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tinyDB = TinyDB(this)
        currentUserId = tinyDB.getLong("userId", 0)

        Log.d(TAG, "Current user ID: $currentUserId")

        initAdapter()
        loadChatMessages()
        setupUI()
    }

    override fun onResume() {
        super.onResume()
        startPolling()
    }

    override fun onPause() {
        super.onPause()
        stopPolling()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPolling()
    }

    private fun initAdapter() {
        chatAdapter = ChatAdapter(chatList)
        chatAdapter.setCurrentUserId(currentUserId)

        binding.chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MyChatActivity)
            adapter = chatAdapter
        }
    }

    private fun setupUI() {
        // Set title
        supportActionBar?.title = "Hỗ trợ khách hàng"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Back button
        binding.backBtn.setOnClickListener {
            finish()
        }

        // Send button
        binding.sendBtn.setOnClickListener {
            sendMessage()
        }

        // Enter key to send
        binding.txtMess.setOnEditorActionListener { _, _, _ ->
            sendMessage()
            true
        }
    }

    private fun loadChatMessages() {
        Log.d(TAG, "Loading chat messages...")

        RetrofitClient.chatApi().getChat().enqueue(object : Callback<ChatResponse> {
            override fun onResponse(call: Call<ChatResponse>, response: Response<ChatResponse>) {
                Log.d(TAG, "Response code: ${response.code()}")

                if (response.isSuccessful) {
                    val chatResponse = response.body()
                    Log.d(TAG, "Response success: ${chatResponse?.success}")

                    if (chatResponse?.success == true) {
                        // Save admin info
                        chatResponse.data?.admin?.let { admin ->
                            adminId = admin.id
                            Log.d(TAG, "Admin info: ${admin.fullname}, ID: ${admin.id}")

                            runOnUiThread {
                                binding.nameTxt.text = "Chat với ${admin.fullname}"
                            }
                        }

                        // Process messages
                        val messages = chatResponse.data?.messages ?: emptyList()
                        Log.d(TAG, "Received ${messages.size} messages")

                        runOnUiThread {
                            chatList.clear()
                            chatList.addAll(messages)
                            chatAdapter.notifyDataSetChanged()

                            if (messages.isNotEmpty()) {
                                lastMessageId = messages.last().id
                                Log.d(TAG, "Last message ID: $lastMessageId")
                            }

                            // Scroll to bottom
                            if (chatList.isNotEmpty()) {
                                binding.chatRecyclerView.scrollToPosition(chatList.size - 1)
                            }
                        }
                    } else {
                        val errorMsg = chatResponse?.message ?: "Không thể tải tin nhắn"
                        Log.e(TAG, errorMsg)
                        showToast(errorMsg)
                    }
                } else {
                    val errorMsg = "Lỗi server: ${response.code()}"
                    Log.e(TAG, errorMsg)
                    try {
                        val errorBody = response.errorBody()?.string()
                        Log.e(TAG, "Error body: $errorBody")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reading error body", e)
                    }
                    showToast(errorMsg)
                }
            }

            override fun onFailure(call: Call<ChatResponse>, t: Throwable) {
                Log.e(TAG, "Load messages error: ${t.message}", t)
                showToast("Lỗi kết nối: ${t.message}")
            }
        })
    }

    private fun sendMessage() {
        val messageText = binding.txtMess.text.toString().trim()
        if (messageText.isEmpty()) {
            showToast("Vui lòng nhập tin nhắn")
            return
        }

        Log.d(TAG, "Sending message: $messageText")
        Log.d(TAG, "Sender ID: $currentUserId, Receiver ID (admin): $adminId")

        val request = SendMessageRequest(
            adminId = adminId,
            message = messageText,
            messageType = "text"
        )
        Log.d(TAG, "Request body: $request")

        val body = hashMapOf<String, Any>(
            "message" to messageText,
            "messageType" to "text",
            "senderId" to currentUserId.toInt(),
            "receiverId" to adminId
        )

        Log.d(TAG, "Request body: $body")

        // Disable send button while sending
        binding.sendBtn.isEnabled = false

        RetrofitClient.chatApi().sendMessage(request).enqueue(object : Callback<BaseResponse> {
            override fun onResponse(call: Call<BaseResponse>, response: Response<BaseResponse>) {
                binding.sendBtn.isEnabled = true

                Log.d(TAG, "Send response code: ${response.code()}")

                if (response.isSuccessful) {
                    val baseResponse = response.body()
                    Log.d(TAG, "Send response success: ${baseResponse?.success}")
                    Log.d(TAG, "Send response message: ${baseResponse?.message}")

                    if (baseResponse?.success == true) {
                        // Clear input
                        binding.txtMess.text.clear()

                        val newMessage = ChatMessage(
                            id = System.currentTimeMillis(),
                            userId = currentUserId,
                            adminId = adminId.toLong(),
                            senderId = currentUserId,
                            message = messageText,
                            messageType = "text",
                            isRead = 0,
                            createdAt = "",
                            chatType = "user_to_admin",
                            senderType = "user"
                        )


                        runOnUiThread {
                            chatList.add(newMessage)
                            chatAdapter.notifyItemInserted(chatList.size - 1)
                            binding.chatRecyclerView.scrollToPosition(chatList.size - 1)
                        }

                        showToast("Đã gửi tin nhắn")

                        // Tải lại tin nhắn để lấy tin nhắn thực từ server
                        loadChatMessages()
                    } else {
                        val errorMsg = baseResponse?.message ?: "Lỗi gửi tin nhắn"
                        showToast(errorMsg)
                    }
                } else {
                    val errorMsg = "Lỗi server: ${response.code()}"
                    Log.e(TAG, errorMsg)
                    try {
                        val errorBody = response.errorBody()?.string()
                        Log.e(TAG, "Error body: $errorBody")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reading error body", e)
                    }
                    showToast(errorMsg)
                }
            }

            override fun onFailure(call: Call<BaseResponse>, t: Throwable) {
                binding.sendBtn.isEnabled = true
                Log.e(TAG, "Send message error: ${t.message}", t)
                showToast("Lỗi gửi tin nhắn: ${t.message}")
            }
        })
    }

    private fun startPolling() {
        Log.d(TAG, "Starting polling...")

        pollingRunnable = object : Runnable {
            override fun run() {
                checkForNewMessages()
                pollingHandler.postDelayed(this, POLLING_INTERVAL)
            }
        }
        pollingHandler.post(pollingRunnable!!)
    }

    private fun stopPolling() {
        Log.d(TAG, "Stopping polling...")
        pollingRunnable?.let {
            pollingHandler.removeCallbacks(it)
        }
    }

    private fun checkForNewMessages() {
        if (lastMessageId == 0L) {
            // Nếu chưa có tin nhắn, load tất cả
            loadChatMessages()
            return
        }

        RetrofitClient.chatApi().getUpdates(lastMessageId).enqueue(object : Callback<ChatResponse> {
            override fun onResponse(call: Call<ChatResponse>, response: Response<ChatResponse>) {
                if (response.isSuccessful) {
                    val chatResponse = response.body()
                    if (chatResponse?.success == true) {
                        val newMessages = chatResponse.data?.messages ?: emptyList()
                        if (newMessages.isNotEmpty()) {
                            Log.d(TAG, "Received ${newMessages.size} new messages")

                            runOnUiThread {
                                val startPosition = chatList.size
                                chatList.addAll(newMessages)
                                chatAdapter.notifyItemRangeInserted(startPosition, newMessages.size)

                                // Update last message ID
                                lastMessageId = newMessages.last().id

                                // Scroll to bottom
                                if (chatList.isNotEmpty()) {
                                    binding.chatRecyclerView.scrollToPosition(chatList.size - 1)
                                }

                                // Show notification for new messages
                                if (newMessages.size == 1) {
                                    showToast("Có tin nhắn mới từ hỗ trợ viên")
                                } else if (newMessages.size > 1) {
                                    showToast("Có ${newMessages.size} tin nhắn mới")
                                }
                            }
                        }
                    }
                }
            }

            override fun onFailure(call: Call<ChatResponse>, t: Throwable) {
                // Silent failure for polling
                Log.d(TAG, "Polling error: ${t.message}")
            }
        })
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
}