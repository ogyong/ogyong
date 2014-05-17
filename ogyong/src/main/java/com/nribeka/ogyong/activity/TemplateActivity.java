package com.nribeka.ogyong.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.nribeka.ogyong.Constants;
import com.nribeka.ogyong.R;

public class TemplateActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_template);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }

    public void updateStatusTemplate(View view) {
        EditText templateEditText = (EditText) findViewById(R.id.template_text_view);
        Button button = (Button) view;
        switch (button.getId()) {
            case R.id.button_white_star:
            case R.id.button_black_star:
            case R.id.button_snow:
            case R.id.button_music:
            case R.id.button_xi:
            case R.id.button_arrow:
            case R.id.button_black_sun:
                int start = Math.max(templateEditText.getSelectionStart(), 0);
                int end = Math.max(templateEditText.getSelectionEnd(), 0);
                templateEditText.getText().replace(Math.min(start, end), Math.max(start, end),
                        button.getText(), 0, button.getText().length());
                break;
            case R.id.button_album:
                templateEditText.append("@album");
                break;
            case R.id.button_artist:
                templateEditText.append("@artist");
                break;
            case R.id.button_track:
                templateEditText.append("@track");
                break;
            default:
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        private EditText templateEditText;

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_template, container, false);
            templateEditText = (EditText) view.findViewById(R.id.template_text_view);
            return view;
        }

        @Override
        public void onResume() {
            super.onResume();
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String currentTemplate = sharedPreferences.getString("status_template", Constants.EMPTY_STRING);
            templateEditText.setText(currentTemplate);
            templateEditText.selectAll();
        }

        @Override
        public void onPause() {
            super.onPause();
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("status_template", templateEditText.getText().toString());
            editor.commit();
        }
    }
}
