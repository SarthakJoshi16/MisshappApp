package com.example.mishappawarenessapp

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.DialogFragment
import com.example.mishappawarenessapp.databinding.DialogEditProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditProfileDialogFragment : DialogFragment() {

    private lateinit var binding: DialogEditProfileBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogEditProfileBinding.inflate(layoutInflater)

        val builder = AlertDialog.Builder(requireContext())
        builder.setView(binding.root)

        builder.setPositiveButton("Save") { _, _ ->
            saveData()
        }
        builder.setNegativeButton("Cancel", null)

        loadUser()
        return builder.create()
    }

    private fun loadUser() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener {
                binding.etName.setText(it.getString("name"))
                binding.etPhone.setText(it.getString("phone"))
                binding.etBio.setText(it.getString("bio"))
            }
    }

    private fun saveData() {
        val uid = auth.currentUser?.uid ?: return

        val map = mapOf(
            "name" to binding.etName.text.toString(),
            "phone" to binding.etPhone.text.toString(),
            "bio" to binding.etBio.text.toString()
        )

        db.collection("users").document(uid)
            .update(map)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Updated successfully!", Toast.LENGTH_SHORT).show()
            }
    }
}

