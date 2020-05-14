package ru.anatomica.cloud_storage.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import java.io.IOException;

import ru.anatomica.cloud_storage.MainActivity;
import ru.anatomica.cloud_storage.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class PlaceholderFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";

    private PageViewModel pageViewModel;

    public static PlaceholderFragment newInstance(int index) {
        PlaceholderFragment fragment = new PlaceholderFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_SECTION_NUMBER, index);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel.class);
        int index = 1;
        if (getArguments() != null) {
            index = getArguments().getInt(ARG_SECTION_NUMBER);
        }
        pageViewModel.setIndex(index);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = null;
        if (pageViewModel.getIndex() == 2) {
            root = inflater.inflate(R.layout.cloud_content, container, false);
            MainActivity.cloudListView = root.findViewById(R.id.cloudListView);
//            final TextView textView = root.findViewById(R.id.section_label);
//            pageViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        } if (pageViewModel.getIndex() == 1) {
            root = inflater.inflate(R.layout.local_content, container, false);
            MainActivity.localListView = root.findViewById(R.id.localListView);
        }
        return root;
    }

}