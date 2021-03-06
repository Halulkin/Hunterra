package com.hunterra.hunterra

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.Nullable
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText


class AddPhotoBottomDialogFragment : BottomSheetDialogFragment() {


    @Nullable
    override fun onCreateView(
        inflater: LayoutInflater,
        @Nullable container: ViewGroup?,
        @Nullable savedInstanceState: Bundle?
    ): View? {
        // get the views and attach the listener
        return inflater.inflate(R.layout.bottom_sheet_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnLogin = view.findViewById<View>(R.id.btnLogin)
        val btnCancel = view.findViewById<View>(R.id.btnCancel)


        val login = view.findViewById<View>(R.id.etLogin) as TextInputEditText
        val mail = view.findViewById<View>(R.id.etEmail) as TextInputEditText
        val pass = view.findViewById<View>(R.id.etPassword) as TextInputEditText



        btnLogin.setOnClickListener {
            this.context?.let { it1 ->
                VolleySingleton.getInstance(view.context)
                    .volleyLoginPost(
                        login.text.toString(), mail.text.toString(), pass.text.toString(),
                        it1
                    )
            }

//            Log.e("*******", "$login $mail $pass")

        }

        btnCancel.setOnClickListener {
            this.dismiss()
        }
    }

    fun showImage(model: Model) {
        val image = view?.findViewById<View>(R.id.ivFotopast)
        if (image != null) {
            image.visibility = View.VISIBLE
            Glide.with(this).load(model.image).into(image as ImageView)
        }
    }

    companion object {
        fun newInstance(): AddPhotoBottomDialogFragment {
            return AddPhotoBottomDialogFragment()
        }
    }
}