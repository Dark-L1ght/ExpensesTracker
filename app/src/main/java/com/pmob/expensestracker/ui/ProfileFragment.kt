package com.pmob.expensestracker.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.pmob.expensestracker.R
import com.pmob.expensestracker.databinding.FragmentProfileBinding
import com.pmob.expensestracker.features.login.LoginActivity

/**
 * Fragment yang digunakan untuk menampilkan dan mengelola profil pengguna.
 * Di dalam fragment ini user bisa melihat data profil, mengubah nama,
 * reset password, dan logout dari aplikasi.
 */
class ProfileFragment : Fragment() {

    /**
     * Binding untuk menghubungkan layout FragmentProfile dengan kode Kotlin.
     * Digunakan agar akses view lebih aman dan rapi.
     */
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    /**
     * Instance FirebaseAuth untuk mengelola autentikasi pengguna.
     */
    private val auth = FirebaseAuth.getInstance()

    /**
     * URL Firebase Realtime Database yang digunakan aplikasi.
     */
    private val databaseUrl =
        "https://pmobakhir-1279e-default-rtdb.asia-southeast1.firebasedatabase.app"

    /**
     * Referensi database ke node "users" untuk menyimpan dan mengambil data user.
     */
    private val dbRef = FirebaseDatabase.getInstance(databaseUrl).getReference("users")

    /**
     * Method untuk menghubungkan fragment dengan layout XML.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Method yang dipanggil setelah view berhasil dibuat.
     * Digunakan untuk menampilkan data user dan mengatur aksi tombol.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        displayUserData()

        binding.btnEditProfile.setOnClickListener { showEditNameDialog() }
        binding.btnResetPassword.setOnClickListener { sendResetPasswordEmail() }
        binding.btnLogout.setOnClickListener { performLogout() }
    }

    /**
     * Method untuk melakukan logout dari Firebase dan akun Google.
     * Setelah logout, user akan diarahkan ke halaman login.
     */
    private fun performLogout() {
        auth.signOut()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .build()

        val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        googleSignInClient.signOut().addOnCompleteListener {
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    /**
     * Method untuk menampilkan data user seperti nama, email,
     * dan foto profil berdasarkan gender.
     */
    private fun displayUserData() {
        val user = auth.currentUser
        if (user != null) {
            binding.tvProfileName.text = user.displayName ?: "Pengguna"
            binding.tvProfileEmail.text = user.email

            dbRef.child(user.uid).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val gender = snapshot.child("gender").value.toString()

                    if (gender == "Laki-laki") {
                        binding.ivProfilePic.setImageResource(R.drawable.ic_male_user)
                    } else if (gender == "Perempuan") {
                        binding.ivProfilePic.setImageResource(R.drawable.ic_female_user)
                    } else {
                        binding.ivProfilePic.setImageResource(R.drawable.ic_others)
                    }
                }
            }.addOnFailureListener {
                binding.ivProfilePic.setImageResource(R.drawable.ic_others)
            }
        }
    }

    /**
     * Menampilkan dialog untuk mengubah nama profil pengguna.
     */
    private fun showEditNameDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Ubah Nama Profil")

        val input = EditText(requireContext())
        input.setText(auth.currentUser?.displayName)
        builder.setView(input)

        builder.setPositiveButton("Simpan") { _, _ ->
            val newName = input.text.toString().trim()
            if (newName.isNotEmpty()) updateProfileName(newName)
        }
        builder.setNegativeButton("Batal", null)
        builder.show()
    }

    /**
     * Method untuk memperbarui nama profil user di Firebase Authentication
     * dan Firebase Realtime Database.
     */
    private fun updateProfileName(newName: String) {
        val user = auth.currentUser ?: return
        val profileUpdates =
            UserProfileChangeRequest.Builder().setDisplayName(newName).build()

        user.updateProfile(profileUpdates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                dbRef.child(user.uid).child("name").setValue(newName)
                binding.tvProfileName.text = newName
                Toast.makeText(context, "Nama berhasil diubah!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Mengirim email reset password ke email user yang sedang login.
     */
    private fun sendResetPasswordEmail() {
        val email = auth.currentUser?.email
        if (email != null) {
            auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        context,
                        "Link reset dikirim ke email",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /**
     * Membersihkan binding saat view dihancurkan
     * untuk mencegah memory leak.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
