package edu.purdue.zhan3050.cnit355final;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.DialogFragment;

import org.jmusixmatch.MusixMatchException;

import java.util.ArrayList;

public class SearchDialog extends DialogFragment {
    public interface SearchDialogListener {
        public void searchArtist(String Name) throws MusixMatchException;
        public void searchSong(String Name, String songName);

    }
    EditText Artist;
    EditText Song;
   SearchDialog.SearchDialogListener listener;
   boolean songSearch = false;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            listener = (SearchDialog.SearchDialogListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(" must implement NoticeDialogListener");
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.search_dialog,null);
        Artist = view.findViewById(R.id.editText);
        Song = view.findViewById(R.id.editText2);
        Song.setEnabled(false);


        builder.setView(view);
        builder.setPositiveButton("Search", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(songSearch){
                    listener.searchSong(Artist.getText().toString(), Song.getText().toString());
                }
                else{
                    try {
                        listener.searchArtist(Artist.getText().toString());
                    } catch (MusixMatchException e) {
                        e.printStackTrace();
                    }

                }

            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        int CheckedIem = 1;
        String[] items ={"Song", "Artist"};
        builder.setSingleChoiceItems(items, CheckedIem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case 0:
                        Song.setEnabled(true);
                        songSearch = true;
                        break;
                    case 1:
                        Song.setEnabled(false);
                        songSearch = false;
                        break;
                }
            }
        });
        return  builder.create();
    }
}
