package com.github.panpf.sketch.sample.ui.view

import androidx.fragment.app.Fragment
import com.github.panpf.assemblyadapter.pager.FragmentItemFactory
import com.github.panpf.sketch.sample.model.ImageDetail

class ImageZoomFragmentItemFactory : FragmentItemFactory<ImageDetail>(ImageDetail::class) {

    override fun createFragment(
        bindingAdapterPosition: Int,
        absoluteAdapterPosition: Int,
        data: ImageDetail
    ): Fragment {
        return ImageZoomFragment().apply {
            arguments = ImageZoomFragmentArgs(data.url).toBundle()
        }
    }
}
