/*
 * Mupen64PlusAE, an N64 emulator for the Android platform
 * 
 * Copyright (C) 2013 Paul Lamb
 * 
 * This file is part of Mupen64PlusAE.
 * 
 * Mupen64PlusAE is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * Mupen64PlusAE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Mupen64PlusAE. If
 * not, see <http://www.gnu.org/licenses/>.
 * 
 * Authors: littleguy77
 */
package paulscode.android.mupen64plusae.compat;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceDialogFragmentCompat;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceFragmentCompat.OnPreferenceStartScreenCallback;
import androidx.preference.PreferenceScreen;

import paulscode.android.mupen64plusae.R;
import paulscode.android.mupen64plusae.compat.AppCompatPreferenceFragment.OnDisplayDialogListener;
import paulscode.android.mupen64plusae.compat.AppCompatPreferenceFragment.OnFragmentCreationListener;
import paulscode.android.mupen64plusae.util.DisplayWrapper;

public abstract class AppCompatPreferenceActivity extends AppCompatActivity implements OnDisplayDialogListener, OnPreferenceStartScreenCallback, OnFragmentCreationListener
{
    public interface OnPreferenceDialogListener
    {
        /**
         * Called while preparing the dialog builder
         * @param context Contextz
         * @param builder dialog builder
         */
        void onPrepareDialogBuilder(Context context, Builder builder);

        /**
         * Called when the dialog view is binded
         */
        void onBindDialogView(View view, FragmentActivity associatedActivity);

        /**
         * Called when the dialog is closed
         */
        void onDialogClosed(boolean result);
    }

    //Generic preference dialog to be used for all preference dialog fragments
    public static class PreferenceDialog extends PreferenceDialogFragmentCompat
    {
        public static PreferenceDialog newInstance(Preference preference)
        {
            PreferenceDialog frag = new PreferenceDialog();
            Bundle args = new Bundle();
            // This has to be the string "key"
            args.putString("key", preference.getKey());
            preference.getSummary();

            frag.setArguments(args);
            return frag;
        }

        @Override
        public void onPrepareDialogBuilder(@NonNull Builder builder)
        {
            super.onPrepareDialogBuilder(builder);

            if (getPreference() instanceof OnPreferenceDialogListener)
            {
                ((OnPreferenceDialogListener) getPreference()).onPrepareDialogBuilder(getActivity(), builder);
            }
            else
            {
                Log.e("PreferenceDialog", "DialogPreference must implement OnPreferenceDialogListener");
            }
        }

        @Override
        public void onBindDialogView(@NonNull View view)
        {
            super.onBindDialogView(view);

            if (getPreference() instanceof OnPreferenceDialogListener)
            {
                ((OnPreferenceDialogListener) getPreference()).onBindDialogView(view, getActivity());
            }
            else
            {
                Log.e("PreferenceDialog", "DialogPreference must implement OnPreferenceDialogListener");
            }
        }

        @Override
        public void onDialogClosed(boolean result)
        {
            if (getPreference() instanceof OnPreferenceDialogListener)
            {

                ((OnPreferenceDialogListener) getPreference()).onDialogClosed(result);
            }
            else
            {
                Log.e("PreferenceDialog", "DialogPreference must implement OnPreferenceDialogListener");
            }
        }
    }

    public static final String STATE_PREFERENCE_FRAGMENT = "STATE_PREFERENCE_FRAGMENT";
    
    private String mSharedPrefsName = null;
    private int mPreferencesResId;

    private int mBottomInset = 0;
    private int mRightInset = 0;
    private int mLeftInset = 0;
    private int mTopInset = 0;
    private Toolbar mToolbar = null;

    // Preference fragment
    private AppCompatPreferenceFragment mPrefFrag = null;

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preference_shell);

        DisplayWrapper.drawBehindSystemBars(this);
        setupToolbar();

        mSharedPrefsName = getSharedPrefsName();
        mPreferencesResId = getSharedPrefsId();

        final FragmentManager fm = getSupportFragmentManager();
        fm.addOnBackStackChangedListener(() -> {
            if (mToolbar != null && fm.getBackStackEntryCount() == 0) {
                mToolbar.setTitle(getTitle());
            }
        });


        Fragment contentFragment = fm.findFragmentById(R.id.prefsContent);

        if (!(contentFragment instanceof AppCompatPreferenceFragment)) {
            mPrefFrag = AppCompatPreferenceFragment.newInstance(mSharedPrefsName, mPreferencesResId, null);
            fm.beginTransaction().replace(R.id.prefsContent, mPrefFrag, STATE_PREFERENCE_FRAGMENT).commit();
        } else {
            mPrefFrag = (AppCompatPreferenceFragment) contentFragment;
        }
    }

    private void setupToolbar() {
        mToolbar = findViewById(R.id.prefsToolbar);
        if (mToolbar == null) {
            return;
        }

        mToolbar.setTitle(getTitle());
        mToolbar.setNavigationIcon(R.drawable.ic_arrow_left);
        mToolbar.setNavigationContentDescription(R.string.inputMapActivity_simulateBack);
        mToolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

    }

    protected abstract String getSharedPrefsName();

    protected abstract int getSharedPrefsId();
    
    public void resetPreferences()
    {        
        getSupportFragmentManager().beginTransaction().remove(mPrefFrag);
        mPrefFrag = AppCompatPreferenceFragment.newInstance(mSharedPrefsName, mPreferencesResId, null);
        getSupportFragmentManager().beginTransaction().replace(R.id.prefsContent, mPrefFrag, STATE_PREFERENCE_FRAGMENT).commit();
    }


    public Preference findPreference(CharSequence key)
    {
        return mPrefFrag.findPreference(key);
    }

    @Override
    public DialogFragment getPreferenceDialogFragment(Preference preference)
    {
        DialogFragment returnFragment = null;

        if (preference instanceof OnPreferenceDialogListener)
        {
            returnFragment = PreferenceDialog.newInstance(preference);
        }
        return returnFragment;
    }

    @Override
    public boolean onPreferenceStartScreen(@NonNull PreferenceFragmentCompat preferenceFragmentCompat,
                                           PreferenceScreen preferenceScreen)
    {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        AppCompatPreferenceFragment fragment = AppCompatPreferenceFragment.newInstance(mSharedPrefsName,
            mPreferencesResId, preferenceScreen.getKey());
        ft.replace(R.id.prefsContent, fragment, preferenceScreen.getKey());
        ft.addToBackStack(null);
        ft.commit();

        if (mToolbar != null) {
            mToolbar.setTitle(preferenceScreen.getTitle());
        }
        
        return true;
    }
    
    protected void OnPreferenceScreenChange(String key)
    {
        //Nothing to do by default
    }

    @Override
    public void onFragmentCreation(AppCompatPreferenceFragment currentFragment)
    {
        if(mPrefFrag != null)
        {
            View fragView = mPrefFrag.getView();
            
            if(fragView != null)
            {
                fragView.setVisibility(View.GONE);
            }
        }

        mPrefFrag = currentFragment;
        
        View fragView = mPrefFrag.getView();
        
        if(fragView != null)
        {
            fragView.setVisibility(View.VISIBLE);
        }
        
        OnPreferenceScreenChange(mPrefFrag.getTag());
    }

    @Override
    public void onViewCreation(View view) {
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {

            mBottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            mRightInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).right;
            mLeftInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).left;
            mTopInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;

            view.setPadding(mLeftInset, 0, mRightInset, mBottomInset);
            
            return insets;
        });

        // Call this a second time since the callback only happens on the first screen
        view.setPadding(mLeftInset, 0, mRightInset, mBottomInset);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected Context getPreferenceManagerContext()
    {
        return mPrefFrag.getPreferenceManager().getContext();
    }

    @SuppressWarnings({"unused", "RedundantSuppression"})
    protected AppCompatPreferenceFragment getPreferenceFragment()
    {
        return mPrefFrag;
    }
    
    

    private static androidx.compose.ui.platform.ComposeView findComposeView( android.view.View view )
    {
        if( view instanceof androidx.compose.ui.platform.ComposeView )
            return (androidx.compose.ui.platform.ComposeView) view;
        if( view instanceof android.view.ViewGroup )
        {
            final android.view.ViewGroup group = (android.view.ViewGroup) view;
            for( int i = 0; i < group.getChildCount(); i++ )
            {
                final androidx.compose.ui.platform.ComposeView found = findComposeView( group.getChildAt( i ) );
                if( found != null )
                    return found;
            }
        }
        return null;
    }

    /**
     * A D-pad or gamepad button press hands the focus to the Compose settings list, so the very first
     * press already moves the highlight.
     */
    @Override
    public boolean dispatchKeyEvent( android.view.KeyEvent event )
    {
        final int keyCode = event.getKeyCode();
        if( event.getAction() == android.view.KeyEvent.ACTION_DOWN
                && ( ( keyCode >= android.view.KeyEvent.KEYCODE_DPAD_UP && keyCode <= android.view.KeyEvent.KEYCODE_DPAD_CENTER )
                || android.view.KeyEvent.isGamepadButton( keyCode ) ) )
        {
            for( final androidx.fragment.app.Fragment fragment : getSupportFragmentManager().getFragments() )
            {
                if( fragment instanceof AppCompatPreferenceFragment && fragment.isVisible() )
                    ( (AppCompatPreferenceFragment) fragment ).onControllerKey();
            }
        }
        return super.dispatchKeyEvent( event );
    }
}
