package com.codinginflow.imagesearchapp.ui.details

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.codinginflow.imagesearchapp.R
import com.codinginflow.imagesearchapp.databinding.FragmentDetailsBinding
import com.codinginflow.imagesearchapp.extension.saveNetworkImageToFileAsync
import kotlinx.android.synthetic.main.fragment_details.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import java.io.File

private const val LOCAL_AUTHORITY = "com.codinginflow.imagesearchapp.fileprovider"

@RuntimePermissions
class DetailsFragment : Fragment(R.layout.fragment_details) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val args by navArgs<DetailsFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentDetailsBinding.bind(view)

        binding.apply {
            val photo = args.photo

            Glide.with(this@DetailsFragment)
                .load(photo.urls.full)
                .error(R.drawable.ic_error)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        progressBar.isVisible = false
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        progressBar.isVisible = false
                        textViewCreator.isVisible = true
                        textViewDescription.isVisible = photo.description != null
                        return false
                    }
                })
                .into(imageView)

            textViewDescription.text = photo.description

            val uri = Uri.parse(photo.user.attributionUrl)
            val intent = Intent(Intent.ACTION_VIEW, uri)

            textViewCreator.apply {
                text = "Photo by ${photo.user.name} on Unsplash"
                setOnClickListener {
                    context.startActivity(intent)
                }
                paint.isUnderlineText = true
            }

            fab_share.apply {
                setOnClickListener {
                    context?.let { currContext ->
                        coroutineScope.launch {
                            shareCurrentImage(currContext, photo.urls.full)
                        }
                    }
                }
            }
        }
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private suspend fun shareCurrentImage(context: Context, photoUrl: String) {
        val tmpFile = saveCurrentImageToTmpFileAsync(context, photoUrl)
        tmpFile?.let { imageFile ->
            val bmpUri = FileProvider.getUriForFile(
                context,
                LOCAL_AUTHORITY, imageFile
            )
            val intent = Intent().apply {
                this.action = Intent.ACTION_SEND
                this.putExtra(Intent.EXTRA_STREAM, bmpUri)
                this.type = getString(R.string.image_type_string)
            }
            startActivity(Intent.createChooser(intent, resources.getText(R.string.send_to)))
        }
    }

    private suspend fun saveCurrentImageToTmpFileAsync(context: Context, url: String): File? {
        val file = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "share_image_" + System.currentTimeMillis() + ".png"
        )
        return image_view.saveNetworkImageToFileAsync(url, file).await()
    }

}