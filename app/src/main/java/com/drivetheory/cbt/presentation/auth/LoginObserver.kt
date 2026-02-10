package com.drivetheory.cbt.presentation.auth

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LoginObserver(
    private val vm: LoginViewModel,
    private val onUpdate: (Boolean, String?) -> Unit
) : DefaultLifecycleObserver {
    override fun onCreate(owner: LifecycleOwner) {
        owner.lifecycleScope.launch {
            vm.state.collectLatest { st ->
                onUpdate(st.success, st.error)
            }
        }
    }
}

