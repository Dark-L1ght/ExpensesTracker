package com.pmob.expensestracker.features.register

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.pmob.expensestracker.databinding.ActivityRegisterBinding
import java.util.*

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        setupUI()
    }

    private fun setupUI() {
        // Setup Exposed Dropdown untuk Gender
        val genders = arrayOf("Laki-laki", "Perempuan")
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, genders)
        (binding.spinnerGender as? AutoCompleteTextView)?.setAdapter(adapter)

        // DatePicker untuk Tanggal Lahir
        binding.etBirthdate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                binding.etBirthdate.setText("$day/${month + 1}/$year")
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.btnRegister.setOnClickListener {
            performRegistration()
        }

        binding.tvLogin.setOnClickListener { finish() }
    }

    private fun performRegistration() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim() // Ambil data telepon
        val pass = binding.etPassword.text.toString().trim()
        val birthDate = binding.etBirthdate.text.toString().trim()
        val gender = binding.spinnerGender.text.toString() // Cara ambil data dari AutoCompleteTextView

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || pass.isEmpty() || birthDate.isEmpty() || gender.isEmpty()) {
            Toast.makeText(this, "Harap lengkapi semua data", Toast.LENGTH_SHORT).show()
            return
        }

        if (pass.length < 6) {
            Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnRegister.isEnabled = false

        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()

                user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                    // Simpan data lengkap ke database termasuk nomor telepon
                    saveUserDataToDatabase(user.uid, name, email, phone, birthDate, gender)
                }
            } else {
                binding.btnRegister.isEnabled = true
                Toast.makeText(this, "Registrasi Gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveUserDataToDatabase(uid: String, name: String, email: String, phone: String, birth: String, gender: String) {
        val databaseUrl = "https://expence-tra-4c82c-default-rtdb.asia-southeast1.firebasedatabase.app"
        val dbRef = FirebaseDatabase.getInstance(databaseUrl).getReference("users")

        val userMap = mapOf(
            "uid" to uid,
            "name" to name,
            "email" to email,
            "phone" to phone, // Simpan nomor telepon
            "birthDate" to birth,
            "gender" to gender,
            "createdAt" to System.currentTimeMillis()
        )

        dbRef.child(uid).setValue(userMap).addOnSuccessListener {
            Toast.makeText(this, "Selamat Datang, $name!", Toast.LENGTH_SHORT).show()
            finish()
        }.addOnFailureListener {
            binding.btnRegister.isEnabled = true
            Toast.makeText(this, "Gagal menyimpan data ke database", Toast.LENGTH_SHORT).show()
        }
    }
}