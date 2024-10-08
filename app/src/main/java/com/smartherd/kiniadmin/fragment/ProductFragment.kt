package com.smartherd.kiniadmin.fragment

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.RadioButton
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
    private var selectedCategory: String = "" // To store the selected category

    private val categoryMap: Map<String, List<String>> = mapOf(
        "Clothing" to listOf("Men", "Women", "Kids"),
        "Electronics" to listOf("Mobile", "Laptop", "Accessories")
    )

    private val subCategoryMap: Map<String, List<String>> = mapOf(
        "Men" to listOf("T-shirt", "Shirt", "Jeans"),
        "Women" to listOf("Shari", "Dress", "Blouse"),
        "Mobile" to listOf("Smartphone", "Feature Phone"),
        "Laptop" to listOf("Gaming", "Ultrabook"),
        "Accessories" to listOf("Watch", "Bracelet")
    )

    companion object {
        const val IMAGE_PICK_CODE = 1000
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        storage = FirebaseStorage.getInstance()
        database = FirebaseDatabase.getInstance().reference
        setupMainCategorySpinner()  // Initialize category spinners

        // Set listener for selecting an image
        binding.selectImage.setOnClickListener {
            pickImageFromGallery()
        }

        // Set listener for upload button
        binding.uploadButton.setOnClickListener {
            val productName = binding.pName.text.toString().trim()
            val productPrice = binding.productPrice.text.toString().trim()

            val mainCategory = binding.mainCategorySpinner.selectedItem?.toString() ?: ""
            val subCategory = binding.subCategorySpinner.selectedItem?.toString() ?: ""
            val itemCategory = binding.itemCategorySpinner.selectedItem?.toString() ?: ""

            // Get selected offer category
            val offerCategory = getSelectedOfferCategory() // Get selected offer category

            if (productName.isNotEmpty() && productPrice.isNotEmpty() && selectedImageUri != null) {
                uploadProductToFirebase(
                    productName,
                    productPrice,
                    "$mainCategory/$subCategory/$itemCategory",
                    offerCategory,
                    selectedImageUri!!
                )
            } else {
                Toast.makeText(requireContext(), "Please fill all fields and select an image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupMainCategorySpinner() {
        val mainCategories = categoryMap.keys.toList()
        val mainAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mainCategories)
        mainAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.mainCategorySpinner.adapter = mainAdapter

        binding.mainCategorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = mainCategories[position]
                setupSubCategorySpinner(selectedCategory)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupSubCategorySpinner(selectedCategory: String) {
        val subCategories = categoryMap[selectedCategory] ?: listOf()
        val subAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, subCategories)
        subAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.subCategorySpinner.adapter = subAdapter
        binding.subCategorySpinner.visibility = View.VISIBLE

        binding.subCategorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedSubCategory = subCategories[position]
                setupItemCategorySpinner(selectedSubCategory)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupItemCategorySpinner(selectedSubCategory: String) {
        val items = subCategoryMap[selectedSubCategory] ?: listOf()
        val itemAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, items)
        itemAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.itemCategorySpinner.adapter = itemAdapter
        binding.itemCategorySpinner.visibility = View.VISIBLE
    }
    private fun getSelectedOfferCategory(): String {
        // Get the selected RadioButton in the RadioGroup
        val selectedId = binding.radioGroupCategory.checkedRadioButtonId
        val selectedRadioButton: RadioButton? = binding.root.findViewById(selectedId)
        return selectedRadioButton?.text.toString()
    }


    private fun pickImageFromGallery() {
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
    private fun uploadProductToFirebase(
        name: String,
        price: String,
        category: String,
        offerCategory: String,
        imageUri: Uri
    ) {
        val uniqueProductId = FirebaseDatabase.getInstance().reference.push().key // Generate a unique ID for the product

        if (uniqueProductId != null) {
            val storageReference = storage.reference.child("product_images/$uniqueProductId.jpg")

            // Upload image to Firebase Storage
            storageReference.putFile(imageUri).addOnSuccessListener {
                storageReference.downloadUrl.addOnSuccessListener { uri ->
                    // After image is uploaded, get the download URL
                    val imageUrl = uri.toString()

                    // Now store product info in Firebase Database
                    val product = Product(uniqueProductId, name, price, category, offerCategory, imageUrl)
                    database.child("Products").child(uniqueProductId).setValue(product)
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
        binding.radioGroupCategory.clearCheck()
        binding.productImageView.setImageResource(R.drawable.image_asset_background) // Reset to a default image
    }
}





































//class ProductFragment : Fragment() {
//
//    private lateinit var binding: FragmentProductBinding
//    private lateinit var storage: FirebaseStorage
//    private lateinit var database: DatabaseReference
//    private var selectedImageUri: Uri? = null // To store the selected image URI
//    private var selectedCategory: String = "" // To store the selected category
//
//    private val categoryMap: Map<String, List<String>> = mapOf(
//        "Clothing" to listOf("Men", "Women", "Kids"),
//        "Electronics" to listOf("Mobile", "Laptop", "Accessories")
//    )
//
//    private val subCategoryMap: Map<String, List<String>> = mapOf(
//        "Men" to listOf("T-shirt", "Shirt", "Jeans"),
//        "Women" to listOf("Shari", "Dress", "Blouse"),
//        "Mobile" to listOf("Smartphone", "Feature Phone"),
//        "Laptop" to listOf("Gaming", "Ultrabook")
//    )
//
//    private val offerCategories = listOf("SpecialProducts", "BestDeals", "BestProducts", "None")
//    companion object {
//        const val IMAGE_PICK_CODE = 1000
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        binding = FragmentProductBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        storage = FirebaseStorage.getInstance()
//        database = FirebaseDatabase.getInstance().reference
//        setupMainCategorySpinner()  // Initialize category spinners
//        setupOfferCategorySpinner() // Initialize offer category spinner
//
//        // Set listener for selecting an image
//        binding.selectImage.setOnClickListener {
//            pickImageFromGallery()
//        }
//
//        // Set listener for category selection
//        binding.radioGroupCategory.setOnCheckedChangeListener { group, checkedId ->
//            selectedCategory = when (checkedId) {
//                R.id.radioSpecialProducts -> "SpecialProducts"
//                R.id.radioBestDeals -> "BestDeals"
//                R.id.radioBestProducts -> "BestProducts"
//                else -> ""
//            }
//        }
//
//        // Set listener for upload button
//        binding.uploadButton.setOnClickListener {
//            val productName = binding.pName.text.toString().trim()
//            val productPrice = binding.productPrice.text.toString().trim()
//
//            val mainCategory = binding.mainCategorySpinner.selectedItem?.toString() ?: ""
//            val subCategory = binding.subCategorySpinner.selectedItem?.toString() ?: ""
//            val itemCategory = binding.itemCategorySpinner.selectedItem?.toString() ?: ""
//
//            // Get selected offer category
//            val offerCategory = binding.offerCategorySpinner.selectedItem?.toString() ?: ""
//
//
//            if (productName.isNotEmpty() && productPrice.isNotEmpty() && selectedImageUri != null) {
//                uploadProductToFirebase(productName, productPrice, "$mainCategory/$subCategory/$itemCategory", offerCategory, selectedImageUri!!)
//            } else {
//                Toast.makeText(requireContext(), "Please fill all fields and select an image", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//    private fun setupMainCategorySpinner() {
//        val mainCategories = categoryMap.keys.toList()
//        val mainAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mainCategories)
//        mainAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//        binding.mainCategorySpinner.adapter = mainAdapter
//
//        binding.mainCategorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
//                val selectedCategory = mainCategories[position]
//                setupSubCategorySpinner(selectedCategory)
//            }
//
//            override fun onNothingSelected(parent: AdapterView<*>?) {}
//        }
//    }
//    private fun setupItemCategorySpinner(selectedSubCategory: String) {
//        val items = subCategoryMap[selectedSubCategory] ?: listOf()
//        val itemAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, items)
//        itemAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//        binding.itemCategorySpinner.adapter = itemAdapter
//        binding.itemCategorySpinner.visibility = View.VISIBLE
//    }
//    private fun setupOfferCategorySpinner() {
//        val offerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, offerCategories)
//        offerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//        binding.offerCategorySpinner.adapter = offerAdapter
//    }
////    private fun setupSubCategorySpinner(selectedCategory: String) {
////        val subCategories = categoryMap[selectedCategory] ?: listOf()
////        val subAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, subCategories)
////        subAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
////        binding.subCategorySpinner.adapter = subAdapter
////        binding.subCategorySpinner.visibility = View.VISIBLE
////
////        binding.subCategorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
////            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
////                val selectedSubCategory = subCategories[position]
////                setupItemCategorySpinner(selectedSubCategory)
////            }
////
////            override fun onNothingSelected(parent: AdapterView<*>?) {}
////        }
////    }
//
//    private fun pickImageFromGallery() {
//        val intent = Intent(Intent.ACTION_PICK)
//        intent.type = "image/*"
//        startActivityForResult(intent, IMAGE_PICK_CODE)
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == IMAGE_PICK_CODE && resultCode == RESULT_OK) {
//            selectedImageUri = data?.data
//            binding.productImageView.setImageURI(selectedImageUri) // Display selected image
//        }
//    }
//
//    // Upload the product information and image to Firebase
//    private fun uploadProductToFirebase(name: String, price: String, category: String, imageUri: Uri) {
//        val uniqueProductId = FirebaseDatabase.getInstance().reference.push().key // Generate a unique ID for the product
//
//        if (uniqueProductId != null) {
//            val storageReference = storage.reference.child("product_images/$uniqueProductId.jpg")
//
//            // Upload image to Firebase Storage
//            storageReference.putFile(imageUri).addOnSuccessListener {
//                storageReference.downloadUrl.addOnSuccessListener { uri ->
//                    // After image is uploaded, get the download URL
//                    val imageUrl = uri.toString()
//
//                    // Now store product info in Firebase Database
//                    val product = Product(uniqueProductId, name, price, category, imageUrl)
//                    database.child(category)
//                        .child(uniqueProductId)
//                        .setValue(product)
//                        .addOnCompleteListener { task ->
//                            if (task.isSuccessful) {
//                                Toast.makeText(requireContext(), "Product uploaded successfully", Toast.LENGTH_SHORT).show()
//                                clearInputs() // Clear the inputs after successful upload
//                            } else {
//                                Toast.makeText(requireContext(), "Failed to upload product", Toast.LENGTH_SHORT).show()
//                            }
//                        }
//                }
//            }.addOnFailureListener {
//                Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show()
//            }
//        } else {
//            Toast.makeText(requireContext(), "Failed to generate unique product ID", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    private fun clearInputs() {
//        binding.pName.text.clear()
//        binding.productPrice.text.clear()
//        binding.radioGroupCategory.clearCheck() // Reset the radio group selection
//        binding.productImageView.setImageResource(R.drawable.image_asset_background) // Reset to a default image
//    }
//}


//class ProductFragment : Fragment() {
//
//    private lateinit var binding: FragmentProductBinding
//    private lateinit var storage: FirebaseStorage
//    private lateinit var database: DatabaseReference
//    private var selectedImageUri: Uri? = null // To store the selected image URI
//
//    companion object {
//        const val IMAGE_PICK_CODE = 1000
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        binding = FragmentProductBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        storage = FirebaseStorage.getInstance()
//        database = FirebaseDatabase.getInstance().reference
//
//        // Set listener for selecting an image
//        binding.selectImage.setOnClickListener {
//            pickImageFromGallery()
//        }
//
//        // Set listener for upload button
//        binding.uploadButton.setOnClickListener {
//            val productName = binding.pName.text.toString().trim()
//            val productPrice = binding.productPrice.text.toString().trim()
//            val productCategory = binding.productCategory.text.toString().trim()
//
//            if (productName.isNotEmpty() && productPrice.isNotEmpty() && productCategory.isNotEmpty() && selectedImageUri != null) {
//                uploadProductToFirebase(productName, productPrice, productCategory, selectedImageUri!!)
//            } else {
//                Toast.makeText(requireContext(), "Please fill all fields and select an image", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//
//    private fun pickImageFromGallery() {
//        //Toast.makeText(requireContext(), "Please fill all fields and select an image", Toast.LENGTH_SHORT).show()
//
//        val intent = Intent(Intent.ACTION_PICK)
//        intent.type = "image/*"
//        startActivityForResult(intent, IMAGE_PICK_CODE)
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == IMAGE_PICK_CODE && resultCode == RESULT_OK) {
//            selectedImageUri = data?.data
//            binding.productImageView.setImageURI(selectedImageUri) // Display selected image
//        }
//    }
//
//    // Upload the product information and image to Firebase
//    private fun uploadProductToFirebase(name: String, price: String, category: String, imageUri: Uri) {
//        val uniqueProductId = FirebaseDatabase.getInstance().reference.push().key // Generate a unique ID for the product
//
//        if (uniqueProductId != null) {
//            val storageReference = storage.reference.child("product_images/$uniqueProductId.jpg")
//
//            // Upload image to Firebase Storage
//            storageReference.putFile(imageUri).addOnSuccessListener {
//                storageReference.downloadUrl.addOnSuccessListener { uri ->
//                    // After image is uploaded, get the download URL
//                    val imageUrl = uri.toString()
//
//                    // Now store product info in Firebase Database
//                    val product = Product(uniqueProductId, name, price, category, imageUrl)
//                    database.child("Products")
//                        .child(uniqueProductId)
//                        .setValue(product)
//                        .addOnCompleteListener { task ->
//                            if (task.isSuccessful) {
//                                Toast.makeText(requireContext(), "Product uploaded successfully", Toast.LENGTH_SHORT).show()
//                                clearInputs() // Clear the inputs after successful upload
//                            } else {
//                                Toast.makeText(requireContext(), "Failed to upload product", Toast.LENGTH_SHORT).show()
//                            }
//                        }
//                }
//            }.addOnFailureListener {
//                Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show()
//            }
//        } else {
//            Toast.makeText(requireContext(), "Failed to generate unique product ID", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    private fun clearInputs() {
//        binding.pName.text.clear()
//        binding.productPrice.text.clear()
//        binding.productCategory.text.clear()
//        binding.productImageView.setImageResource(R.drawable.image_asset_background) // Reset to a default image
//    }
//}
