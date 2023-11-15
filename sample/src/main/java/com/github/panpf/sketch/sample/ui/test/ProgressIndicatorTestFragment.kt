/*
 * Copyright (C) 2022 panpf <panpfpanpf@outlook.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.panpf.sketch.sample.ui.test

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.transition.TransitionInflater
import com.github.panpf.sketch.request.DisplayRequest
import com.github.panpf.sketch.sample.R
import com.github.panpf.sketch.sample.databinding.ProgressIndicatorTestFragmentBinding
import com.github.panpf.sketch.sample.ui.base.ToolbarBindingFragment
import com.github.panpf.sketch.viewability.ProgressIndicatorAbility
import com.github.panpf.sketch.viewability.ViewAbilityContainer
import com.github.panpf.sketch.viewability.showMaskProgressIndicator
import com.github.panpf.sketch.viewability.showRingProgressIndicator
import com.github.panpf.sketch.viewability.showSectorProgressIndicator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class ProgressIndicatorTestFragment :
    ToolbarBindingFragment<ProgressIndicatorTestFragmentBinding>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            sharedElementEnterTransition = TransitionInflater.from(requireContext())
                .inflateTransition(R.transition.my_move)
        }
    }

    override fun onViewCreated(
        toolbar: androidx.appcompat.widget.Toolbar,
        binding: ProgressIndicatorTestFragmentBinding,
        savedInstanceState: Bundle?
    ) {
        toolbar.title = "ProgressIndicator"

        if (VERSION.SDK_INT >= 21) {
            binding.testIndicatorTestImage1.setImageResource(R.drawable.im_placeholder)
            binding.testIndicatorTestImage2.setImageResource(R.drawable.im_placeholder)
            binding.testIndicatorTestImage3.setImageResource(R.drawable.im_placeholder)
        } else {
            binding.testIndicatorTestImage1.setImageResource(R.drawable.im_placeholder_noicon)
            binding.testIndicatorTestImage2.setImageResource(R.drawable.im_placeholder_noicon)
            binding.testIndicatorTestImage3.setImageResource(R.drawable.im_placeholder_noicon)
        }
        binding.testIndicatorTestImage1.showMaskProgressIndicator()
        binding.testIndicatorTestImage2.showSectorProgressIndicator()
        binding.testIndicatorTestImage3.showRingProgressIndicator()

        viewLifecycleOwner.lifecycleScope.launch {
            var progress = 0L
            val request = DisplayRequest(requireContext(), "http://sample.com/sample.jpeg")
            binding.testIndicatorTestImage1.progressIndicatorAbility.onRequestStart(request)
            binding.testIndicatorTestImage2.progressIndicatorAbility.onRequestStart(request)
            binding.testIndicatorTestImage3.progressIndicatorAbility.onRequestStart(request)
            while (progress <= 100) {
                delay(Random.nextLong(500, 2000))
                progress += 20
                binding.testIndicatorTestImage1.progressIndicatorAbility
                    .onUpdateRequestProgress(request, 100, progress)
                binding.testIndicatorTestImage2.progressIndicatorAbility
                    .onUpdateRequestProgress(request, 100, progress)
                binding.testIndicatorTestImage3.progressIndicatorAbility
                    .onUpdateRequestProgress(request, 100, progress)
            }
        }
    }

    private val ViewAbilityContainer.progressIndicatorAbility: ProgressIndicatorAbility
        get() = viewAbilityList.find { it is ProgressIndicatorAbility }!!
            .let { it as ProgressIndicatorAbility }
}