package com.smartherd.kiniadmin.fragment

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.smartherd.kiniadmin.R
import com.smartherd.kiniadmin.data.Product
import com.smartherd.kiniadmin.databinding.FragmentProductBinding
import java.util.UUID

class ProductFragment : Fragment() {

    private lateinit var binding: FragmentProductBinding
    private lateinit var storage: FirebaseStorage
    private lateinit var database: DatabaseReference
    private var selectedImageUri: Uri? = null // To store the selected image URI

    companion object {
        const val IMAGE_PICK_CODE = 1000
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        storage = FirebaseStorage.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Set listener for selecting an image
        binding.selectImage.setOnClickListener {
            pickImageFromGallery()
        }

        // Set listener for upload button
        binding.uploadButton.setOnClickListener {
            val productName = binding.pName.text.toString().trim()
            val productPrice = binding.productPrice.text.toString().trim()
            val productCategory = binding.productCategory.text.toString().trim()

            if (productName.isNotEmpty() && productPrice.isNotEmpty() && productCategory.isNotEmpty() && selectedImageUri != null) {
                uploadProductToFirebase(productName, productPrice, productCategory, selectedImageUri!!)
            } else {
                Toast.makeText(requireContext(), "Please fill all fields and select an image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun pickImageFromGallery() {
        //Toast.makeText(requireContext(), "Please fill all fields and select an image", Toast.LENGTH_SHORT).show()

        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICK_CODE && resultCode == RESULT_OK) {
            selectedImageUri = data?.data
            binding.productImageView.setImageURI(selectedImageUri) // Display selected image
        }
    }

    // Upload the product information and image to Firebase
    private fun uploadProductToFirebase(name: String, price: String, category: String, imageUri: Uri) {
        val uniqueProductId = FirebaseDatabase.getInstance().reference.push().key // Generate a unique ID for the product

        if (uniqueProductId != null) {
            val storageReference = storage.reference.child("product_images/$uniqueProductId.jpg")

            // Upload image to Firebase Storage
            storageReference.putFile(imageUri).addOnSuccessListener {
                storageReference.downloadUrl.addOnSuccessListener { uri ->
                    // After image is uploaded, get the download URL
                    val imageUrl = uri.toString()

                    // Now store product info in Firebase Database
                    val product = Product(uniqueProductId, name, price, category, imageUrl)
                    database.child("Products")
                        .child(uniqueProductId)
                        .setValue(product)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(requireContext(), "Product uploaded successfully", Toast.LENGTH_SHORT).show()
                                clearInputs() // Clear the inputs after successful upload
                            } else {
                                Toast.makeText(requireContext(), "Failed to upload product", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Failed to generate unique product ID", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearInputs() {
        binding.pName.text.clear()
        binding.productPrice.text.clear()
        binding.productCategory.text.clear()
        binding.productImageView.setImageResource(R.drawable.image_asset_background) // Reset to a default image
    }
}
