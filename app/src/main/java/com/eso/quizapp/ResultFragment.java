package com.eso.quizapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import static com.eso.quizapp.utils.Common.QUIZLIST;
import static com.eso.quizapp.utils.Common.RESULTS;


/**
 * A simple {@link Fragment} subclass.
 */
public class ResultFragment extends Fragment {

    private NavController navController;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth auth;
    private String quizId;
    private TextView mResultsTitle;
    private ProgressBar mResultsProgress;
    private TextView mResultsPercent;
    private TextView mResultsCorrect;
    private TextView mResultsWrong;
    private TextView mResultsMissed;
    private TextView mResultsCorrectText;
    private TextView mResultsWrongText;
    private TextView mResultsMissedText;
    private Button mResultsHomeBtn;
    private String currentUserId;

    public ResultFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_result, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        firebaseFirestore = FirebaseFirestore.getInstance();
        if (getArguments() != null)
            quizId = ResultFragmentArgs.fromBundle(getArguments()).getQuizId();
        auth = FirebaseAuth.getInstance();
        //Get User Id
        if (auth.getCurrentUser() != null)
            currentUserId = auth.getCurrentUser().getUid();
        else {
            //Go Back To Home Page
        }

        //Get Results
        firebaseFirestore.collection(QUIZLIST).document(quizId).collection(RESULTS)
                .document(currentUserId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()){
                    DocumentSnapshot result = task.getResult();
                    Long correct = result.getLong("correct");
                    Long wrong = result.getLong("wrong");
                    Long missed = result.getLong("unanswered");

                    mResultsCorrect.setText(correct.toString());
                    mResultsCorrect.setText(wrong.toString());
                    mResultsCorrect.setText(missed.toString());

                    //Calculate Progress
                    Long total = correct + wrong + missed;
                    Long percent = (correct *100)/total;
                    mResultsPercent.setText(percent + "%");
                    mResultsProgress.setProgress(percent.intValue());
                }
            }
        });
        mResultsTitle = view.findViewById(R.id.results_title);
        mResultsProgress = view.findViewById(R.id.results_progress);
        mResultsPercent = view.findViewById(R.id.results_percent);
        mResultsCorrect = view.findViewById(R.id.results_correct);
        mResultsWrong = view.findViewById(R.id.results_wrong);
        mResultsMissed = view.findViewById(R.id.results_missed);
        mResultsCorrectText = view.findViewById(R.id.results_correct_text);
        mResultsWrongText = view.findViewById(R.id.results_wrong_text);
        mResultsMissedText = view.findViewById(R.id.results_missed_text);
        mResultsHomeBtn = view.findViewById(R.id.results_home_btn);
        mResultsHomeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navController.navigate(R.id.action_resultFragment_to_listFragment);
            }
        });
    }
}
