package com.example.app.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.app.Adapter.CommentAdapter
import com.example.app.Adapter.PicAdapter
import com.example.app.Adapter.SelectedModelAdapter
import com.example.app.Model.CommentModel
import com.example.app.Model.ItemsModel
import com.example.app.databinding.ActivityDetailBinding
import com.example.project1762.Helper.ManagmentCart
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class DetailActivity : BaseActivity() {

    private lateinit var binding: ActivityDetailBinding
    private lateinit var item: ItemsModel
    private lateinit var itemId: String
    private lateinit var managmentCart: ManagmentCart
    private lateinit var firebaseDatabase: DatabaseReference
    private lateinit var commentAdapter: CommentAdapter
    private lateinit var auth: FirebaseAuth
    private val commentList = ArrayList<CommentModel>()
    private var numberOrder = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        managmentCart = ManagmentCart(this)
        firebaseDatabase = FirebaseDatabase.getInstance().reference

        getBundle()
        initList()
        setupCommentRecyclerView()
        loadComments()

        // Xử lý thêm vào giỏ hàng
        binding.addToCartBtn.setOnClickListener {
            item.numberInCart = numberOrder
            managmentCart.insertItem(item)
        }

        // Xử lý gửi bình luận
        binding.submitCommentBtn.setOnClickListener {
            val commentText = binding.commentInput.text.toString()
            if (commentText.isNotEmpty()) {
                saveCommentToFirebase(commentText)
            } else {
                Toast.makeText(this, "Vui lòng nhập nội dung", Toast.LENGTH_SHORT).show()
            }
        }

        binding.backBtn.setOnClickListener { finish() }
        binding.cartBtn.setOnClickListener { startActivity(Intent(this, CartActivity::class.java)) }
    }

    private fun getBundle() {
        item = intent.getParcelableExtra("object")!!

        itemId = intent.getStringExtra("itemKey") ?: ""
        Log.d("DetailActivity", "itemId: $itemId")

        binding.titleTxt.text = item.title
        binding.derscriptionTxt.text = item.description
        binding.priceTxt.text = "$${item.price}"
        binding.raitingTxt.text = "${item.rating} Rating"
    }

    private fun initList() {
        val modelList = ArrayList<String>().apply { addAll(item.model) }
        binding.modelList.adapter = SelectedModelAdapter(modelList)
        binding.modelList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val picList = ArrayList<String>().apply { addAll(item.picUrl) }
        Glide.with(this).load(picList[0]).into(binding.img)

        binding.picList.adapter = PicAdapter(picList) { selectedImageUrl ->
            Glide.with(this).load(selectedImageUrl).into(binding.img)
        }
        binding.picList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }

    private fun setupCommentRecyclerView() {
        commentAdapter = CommentAdapter(commentList)
        binding.modelComment.apply {
            layoutManager = LinearLayoutManager(this@DetailActivity)
            adapter = commentAdapter
        }
    }

    private fun loadComments() {
        if (itemId.isEmpty()) return

        firebaseDatabase.child("Items").child(itemId).child("comments")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    commentList.clear()
                    for (commentSnapshot in snapshot.children) {
                        val comment = commentSnapshot.getValue(CommentModel::class.java)
                        comment?.let { commentList.add(it) }
                    }
                    commentAdapter.notifyDataSetChanged()

                    if (commentList.isEmpty()) {
                        Toast.makeText(this@DetailActivity, "Chưa có bình luận nào", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@DetailActivity, "Lỗi khi tải bình luận: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun saveCommentToFirebase(commentText: String) {

        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val userEmail = firebaseUser?.email ?: "unknown@email.com"

        val usersRef = FirebaseDatabase.getInstance().getReference("Users")
        usersRef.get().addOnSuccessListener { dataSnapshot ->
            var userName = ""
            for(userSnapshot  in dataSnapshot.children){
                val authEmail = userSnapshot.child("auth_email").getValue(String::class.java)
                if(authEmail == userEmail){
                    userName = userSnapshot.child("profile_name").getValue(String::class.java) ?: ""
                    break
                }
            }

            val comment = CommentModel(
                userName = userName,
                commentText = commentText,
                imageUrl = null,
                videoUrl = null
            )

            val commentsRef = firebaseDatabase.child("Items").child(itemId).child("comments")

            // Dùng push() để cho phép gửi nhiều bình luận
            commentsRef.push().setValue(comment)
                .addOnSuccessListener {
                    Toast.makeText(this@DetailActivity, "Bình luận đã được gửi", Toast.LENGTH_SHORT).show()
                    binding.commentInput.text.clear()
                }
                .addOnFailureListener {
                    Toast.makeText(this@DetailActivity, "Lỗi khi gửi bình luận", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
