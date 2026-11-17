package paulscode.android.mupen64plusae.dialog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.compose.ui.platform.ComposeView;
import paulscode.android.mupen64plusae.ui.ConfirmUi;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog.Builder;
import android.util.Log;

public class ConfirmationDialog extends DialogFragment
{
    private static final String STATE_ID = "STATE_ID";
    private static final String STATE_TITLE = "STATE_TITLE";
    private static final String STATE_MESSAGE = "STATE_MESSAGE";
    
    private int mId = 0;
    
    /**
     * The listener interface for handling confirmations.
     */
    public interface PromptConfirmListener
    {
        /**
         * Handle the user's confirmation.
         */
        void onPromptDialogClosed(int id, int which);
    }

    public static ConfirmationDialog newInstance(int id, String title, String message)
    {
        ConfirmationDialog frag = new ConfirmationDialog();
        Bundle args = new Bundle();
        args.putInt(STATE_ID, id);
        args.putString(STATE_TITLE, title);
        args.putString(STATE_MESSAGE, message);

        frag.setArguments(args);
        return frag;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        // The Compose view drawn in onCreateView provides the scrim and the card
        return new Dialog(requireContext(), android.R.style.Theme_Translucent_NoTitleBar);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState)
    {
        final String title = getArguments() != null ? getArguments().getString(STATE_TITLE) : "";
        final String message = getArguments() != null ? getArguments().getString(STATE_MESSAGE) : "";
        mId = getArguments() != null ? getArguments().getInt(STATE_ID) : 0;

        final ComposeView view = new ComposeView(requireContext());
        ConfirmUi.attach(view, title == null ? "" : title, message == null ? "" : message,
                () -> notifyListener(DialogInterface.BUTTON_POSITIVE),
                () -> notifyListener(DialogInterface.BUTTON_NEGATIVE));
        return view;
    }

    private void notifyListener(int which)
    {
        if (getActivity() instanceof PromptConfirmListener)
        {
            ((PromptConfirmListener) getActivity()).onPromptDialogClosed(mId, which);
        }
        else
        {
            Log.e("ConfirmationDialog", "Activity doesn't implement PromptConfirmListener");
        }
        dismissAllowingStateLoss();
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog)
    {
        if (getActivity() instanceof PromptConfirmListener)
        {
            ((PromptConfirmListener) getActivity()).onPromptDialogClosed(mId, DialogInterface.BUTTON_NEGATIVE);
        }
        else
        {
            Log.e("ConfirmationDialog", "Activity doesn't implement PromptConfirmListener");
        }
    }
}