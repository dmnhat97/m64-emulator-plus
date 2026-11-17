package paulscode.android.mupen64plusae.compat;

import android.content.SharedPreferences;
import android.widget.FrameLayout;
import androidx.compose.ui.platform.ComposeView;
import androidx.fragment.app.FragmentActivity;
import paulscode.android.mupen64plusae.ui.InputMode;
import paulscode.android.mupen64plusae.ui.PrefsState;
import paulscode.android.mupen64plusae.ui.PrefsUi;
import androidx.preference.ListPreference;
import androidx.preference.EditTextPreference;
import paulscode.android.mupen64plusae.preference.CompatListPreference;
import paulscode.android.mupen64plusae.preference.ProfilePreference;
import paulscode.android.mupen64plusae.preference.SeekBarPreference;
import paulscode.android.mupen64plusae.ui.GxDialogSpec;
import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnChildAttachStateChangeListener;

import paulscode.android.mupen64plusae.R;

public class AppCompatPreferenceFragment extends PreferenceFragmentCompat
{
    public interface OnDisplayDialogListener
    {
        /**
         * Called when a preference dialog is being displayed. This must return
         * the appropriate DialogFragment for the preference.
         *
         * @param preference
         *            The preference dialog
         * @return The dialog fragment for the preference
         */
        DialogFragment getPreferenceDialogFragment(Preference preference);
    }

    public interface OnFragmentCreationListener
    {
        /**
         * Called when a preference dialog is being displayed. This must return
         * the appropriate DialogFragment for the preference.
         * 
         * @param currentFragment Current fragment
         */
        void onFragmentCreation(AppCompatPreferenceFragment currentFragment);

        /**
         * Callback when the fragment views are created
         * @param view Fragment main view
         */
        void onViewCreation(View view);
    }

    private static final String STATE_SHATED_PREFS_NAME = "STATE_SHATED_PREFS_NAME";
    private static final String STATE_RESOURCE_ID = "STATE_RESOURCE_ID";
    
    private boolean mHasFocusBeenSet = false;
    private final PrefsState mPrefsState = new PrefsState();
    private ComposeView mCompose = null;
    private final SharedPreferences.OnSharedPreferenceChangeListener mPrefsListener =
            (sharedPreferences, key) -> mPrefsState.bump();

    public static AppCompatPreferenceFragment newInstance(String sharedPrefsName, int resourceId, String rootKey)
    {
        AppCompatPreferenceFragment frag = new AppCompatPreferenceFragment();
        Bundle args = new Bundle();
        args.putString(STATE_SHATED_PREFS_NAME, sharedPrefsName);
        args.putInt(STATE_RESOURCE_ID, resourceId);
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, rootKey);

        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
    {
        Bundle arguments = getArguments();

        if (arguments == null) {
            return;
        }
        
        final String sharedPrefsName = arguments.getString(STATE_SHATED_PREFS_NAME);
        final int resourceId = arguments.getInt(STATE_RESOURCE_ID);

        // Load the preferences from an XML resource

        if (sharedPrefsName != null)
        {
            getPreferenceManager().setSharedPreferencesName(sharedPrefsName);
        }

        if (rootKey == null)
        {
            addPreferencesFromResource(resourceId);
        }
        else
        {
            setPreferencesFromResource(resourceId, rootKey);
        }
    }
    
    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        if (getActivity() instanceof OnFragmentCreationListener)
        {
            ((OnFragmentCreationListener) getActivity()).onFragmentCreation(this);
        }
        
        mHasFocusBeenSet = false;

        final View base = super.onCreateView(inflater, container, savedInstanceState);

        // The Compose settings UI renders the live preference tree. The original RecyclerView stays in
        // the hierarchy (hidden) because the rest of the code and the preference library still use it.
        final Context context = inflater.getContext();
        final FrameLayout wrapper = new FrameLayout(context);
        wrapper.addView(base, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        base.setVisibility(View.INVISIBLE);

        final ComposeView compose = new ComposeView(context);
        compose.setFocusable(true);
        compose.setFocusableInTouchMode(true);
        PrefsUi.attach(compose, mPrefsState, this::getPreferenceScreen, () -> {
            final FragmentActivity activity = getActivity();
            if (activity != null)
                activity.getOnBackPressedDispatcher().onBackPressed();
        });
        wrapper.addView(compose, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mCompose = compose;

        return wrapper;
    }

    /** Called by the host activity when a D-pad / gamepad key is pressed. */
    public void onControllerKey()
    {
        mPrefsState.setInputMode( InputMode.GAMEPAD );
        if( mCompose != null && !mCompose.hasFocus() )
            mCompose.requestFocus();
    }

    @Override
    public void onStart()
    {
        super.onStart();
        final SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        if (prefs != null)
            prefs.registerOnSharedPreferenceChangeListener(mPrefsListener);
        mPrefsState.bump();
        if (mCompose != null)
            mCompose.requestFocus();
    }

    @Override
    public void onStop()
    {
        final SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        if (prefs != null)
            prefs.unregisterOnSharedPreferenceChangeListener(mPrefsListener);
        super.onStop();
    }

    private static java.util.List<String> entriesOf(ListPreference preference)
    {
        final java.util.ArrayList<String> entries = new java.util.ArrayList<>();
        final CharSequence[] raw = preference.getEntries();
        if (raw != null)
            for (final CharSequence entry : raw)
                entries.add(String.valueOf(entry));
        return entries;
    }

    /** Shows the Compose version of the dialog for the preference types it supports. */
    private boolean showComposeDialog(Preference preference)
    {
        if (preference instanceof CompatListPreference)
        {
            final CompatListPreference list = (CompatListPreference) preference;
            mPrefsState.setDialog(new GxDialogSpec.Choice(String.valueOf(list.getTitle()), entriesOf(list),
                    list.findIndexOfValue(list.getCurrentValue()), null, null,
                    index -> list.setValue(list.getEntryValues()[index].toString())));
            return true;
        }
        if (preference instanceof ProfilePreference && ((ProfilePreference) preference).hasManager())
        {
            final ProfilePreference profile = (ProfilePreference) preference;
            mPrefsState.setDialog(new GxDialogSpec.Choice(String.valueOf(profile.getTitle()), entriesOf(profile),
                    profile.findIndexOfValue(profile.getCurrentValue(null)),
                    getString(R.string.profile_manage_profiles), profile::launchManager,
                    index -> profile.setValue(profile.getEntryValues()[index].toString())));
            return true;
        }
        if (preference instanceof SeekBarPreference)
        {
            final SeekBarPreference seek = (SeekBarPreference) preference;
            mPrefsState.setDialog(new GxDialogSpec.Slider(String.valueOf(seek.getTitle()), seek.getMinValue(),
                    seek.getMaxValue(), seek.getStepSize(), seek.getValue(), seek::getValueString, value -> {
                        if (seek.callChangeListener(value))
                            seek.setValue(value);
                    }));
            return true;
        }
        if (preference instanceof EditTextPreference)
        {
            final EditTextPreference edit = (EditTextPreference) preference;
            final String current = edit.getText();
            mPrefsState.setDialog(new GxDialogSpec.Text(String.valueOf(edit.getTitle()), current == null ? "" : current, text -> {
                if (edit.callChangeListener(text))
                    edit.setText(text);
            }));
            return true;
        }
        return false;
    }

    @Override
    public void onDisplayPreferenceDialog(@NonNull Preference preference)
    {
        if (showComposeDialog(preference))
            return;

        DialogFragment fragment = null;

        if (getActivity() instanceof OnDisplayDialogListener)
        {
            fragment = ((OnDisplayDialogListener) getActivity()).getPreferenceDialogFragment(preference);

            if (fragment != null)
            {
                // TODO: Correct this deprecation once setTargetFragment is no longer required by
                // PreferenceDialogFragmentCompat in Android preference library
                fragment.setTargetFragment(this, 0);

                try {
                    FragmentManager fragmentManager = getParentFragmentManager();
                    fragment.show(fragmentManager, "androidx.preference.PreferenceFragment.DIALOG");
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }
        }

        if (fragment == null)
        {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        // Set the default white background in the view so as to avoid
        // transparency
        Context context = getContext();
        if (context != null) {
            view.setBackgroundColor(ContextCompat.getColor(context, R.color.mupen_black));
        }

        if (getActivity() instanceof OnFragmentCreationListener)
        {
            ((OnFragmentCreationListener) getActivity()).onViewCreation(getListView());
        }

        // Detect when a view is added to the preference fragment and request focus if it's the first view
        final RecyclerView recyclerView = getListView();

        final RecyclerView.Adapter<?> adapter = recyclerView.getAdapter();

        recyclerView.addOnChildAttachStateChangeListener(new OnChildAttachStateChangeListener()
        {
            @Override
            public void onChildViewAttachedToWindow(@NonNull View childView)
            {
                final LinearLayoutManager layoutManager = (LinearLayoutManager)recyclerView.getLayoutManager();

                //Prevent scrolling past the top
                childView.setOnKeyListener((v, keyCode, event) -> {
                    View focusedChild;
                    if (layoutManager != null && adapter != null) {
                        focusedChild = layoutManager.getFocusedChild();
                        int selectedPos;
                        if (focusedChild != null) {
                            selectedPos = recyclerView.getChildAdapterPosition(focusedChild);

                            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                                switch(keyCode) {
                                    case KeyEvent.KEYCODE_DPAD_DOWN:
                                        return !(selectedPos < adapter.getItemCount() - 1);
                                    case KeyEvent.KEYCODE_DPAD_UP:
                                        return selectedPos == 0;
                                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                                        return true;
                                }
                                return false;
                            }
                            return false;
                        }
                    }

                    return false;
                });

                //Make sure all views are focusable
                childView.setFocusable(true);

                if(adapter != null && layoutManager != null && adapter.getItemCount() > 0)
                {
                    int firstItem = layoutManager.findFirstCompletelyVisibleItemPosition();

                    //Get focus on the first visible item the first time it's displayed
                    if (firstItem != -1) {
                        RecyclerView.ViewHolder holder = recyclerView.findViewHolderForItemId(adapter.getItemId(firstItem));
                        if(holder != null)
                        {
                            if (!mHasFocusBeenSet)
                            {
                                mHasFocusBeenSet = true;
                                holder.itemView.requestFocus();
                            }
                        }
                    }
                }
            }

            @Override
            public void onChildViewDetachedFromWindow(@NonNull View arg0)
            {
                // Nothing to do here
            }
        });
    }
}