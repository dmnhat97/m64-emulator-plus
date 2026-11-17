/*
 * XM64 Emulator Plus, a modified version of Mupen64PlusAE
 * <p>
 * Copyright (C) 2026 XM64 Emulator Plus contributors
 * <p>
 * This file is part of XM64 Emulator Plus.
 * <p>
 * XM64 Emulator Plus is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * <p>
 * XM64 Emulator Plus is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General
 * Public License along with XM64 Emulator Plus. If not, see <http://www.gnu.org/licenses/>.
 */
package paulscode.android.mupen64plusae.dialog;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.TypedValue;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog.Builder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import paulscode.android.mupen64plusae.R;

/**
 * Shows the open source notice (fork attribution, third-party licenses, GPL text, link to source).
 */
public final class LicensesDialog
{
    private static final String NOTICE_ASSET = "licenses/NOTICE.txt";
    private static final String GPL_ASSET = "licenses/gpl-3.0.txt";

    private LicensesDialog() {}

    public static void show(final Context context)
    {
        showText(context, context.getString(R.string.menuItem_licenses), NOTICE_ASSET, true);
    }

    private static void showText(final Context context, final String title, final String asset, final boolean main)
    {
        final TextView text = new TextView(context);
        text.setText(readAsset(context, asset));
        text.setTypeface(android.graphics.Typeface.MONOSPACE);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        text.setTextIsSelectable(true);
        Linkify.addLinks(text, Linkify.WEB_URLS);
        text.setMovementMethod(LinkMovementMethod.getInstance());
        final int pad = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16,
                context.getResources().getDisplayMetrics());
        text.setPadding(pad, pad, pad, pad);

        final ScrollView scroll = new ScrollView(context);
        scroll.addView(text);

        final Builder builder = new Builder(context).setTitle(title).setView(scroll)
                .setPositiveButton(android.R.string.ok, null);

        if (main) {
            builder.setNeutralButton(R.string.licenses_sourceCode, (dialog, which) -> openSource(context));
            builder.setNegativeButton(R.string.licenses_fullGpl, (dialog, which) ->
                    showText(context, context.getString(R.string.licenses_fullGpl), GPL_ASSET, false));
        }
        builder.create().show();
    }

    private static void openSource(final Context context)
    {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.uri_sourceCode))));
        } catch (final ActivityNotFoundException ignored) {
            // No browser available
        }
    }

    private static String readAsset(final Context context, final String asset)
    {
        try (InputStream in = context.getAssets().open(asset)) {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toString(StandardCharsets.UTF_8.name());
        } catch (final IOException e) {
            return "";
        }
    }
}
