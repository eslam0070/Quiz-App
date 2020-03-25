package com.eso.quizapp;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.eso.quizapp.model.QuestionsModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static com.eso.quizapp.utils.Common.QUESTIONS;
import static com.eso.quizapp.utils.Common.QUIZLIST;


/**
 * A simple {@link Fragment} subclass.
 */
@SuppressLint("SetTextI18n")
@RequiresApi(api = Build.VERSION_CODES.M)
public class QuizFragment extends Fragment implements View.OnClickListener{

    private NavController navController;

    //UI Elements
    private TextView quizTitle;
    private Button optionOneBtn;
    private Button optionTwoBtn;
    private Button optionThreeBtn;
    private Button nextBtn;
    private ImageButton closeBtn;
    private TextView questionFeedback;
    private TextView questionText;
    private TextView questionTime;
    private ProgressBar questionProgress;
    private TextView questionNumber;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth auth;
    private List<QuestionsModel> allQuestionsList = new ArrayList<>();
    private List<QuestionsModel> questionsToAnswer = new ArrayList<>();
    private Long totalQuestionsToAnswer = 0L;
    private String quizId,currentUserId,quizName;
    private boolean canAnswer = false;
    private int currentQuestion = 0;
    private CountDownTimer countDownTimer;
    private int currentAnswers = 0,wrongAnswers = 0,notAnswered = 0;
    public QuizFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_quiz, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        firebaseFirestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        //Get User Id
        if (auth.getCurrentUser() != null)
            currentUserId = auth.getCurrentUser().getUid();
        else {
            //Go Back To Home Page
        }
        //UI Initialize
        quizTitle = view.findViewById(R.id.quiz_title);
        optionOneBtn = view.findViewById(R.id.quiz_option_one);
        optionTwoBtn = view.findViewById(R.id.quiz_option_two);
        optionThreeBtn = view.findViewById(R.id.quiz_option_three);
        nextBtn = view.findViewById(R.id.quiz_next_btn);
        questionFeedback = view.findViewById(R.id.quiz_question_feedback);
        questionText = view.findViewById(R.id.quiz_question);
        questionTime = view.findViewById(R.id.quiz_question_time);
        questionProgress = view.findViewById(R.id.quiz_question_progress);
        questionNumber = view.findViewById(R.id.quiz_question_number);
        closeBtn = view.findViewById(R.id.quiz_close_btn);
        if (getArguments() != null){
            quizId = QuizFragmentArgs.fromBundle(getArguments()).getQuizId();
            quizName = QuizFragmentArgs.fromBundle(getArguments()).getQuizName();
            totalQuestionsToAnswer = QuizFragmentArgs.fromBundle(getArguments()).getTotalQuestions();
        }

        //Query Firestore Data
        firebaseFirestore.collection(QUIZLIST)
                .document(quizId).collection(QUESTIONS)
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    allQuestionsList = Objects.requireNonNull(task.getResult()).toObjects(QuestionsModel.class);
                    pickQuestions();
                    loadUI();
                } else {
                    quizTitle.setText("Error : " + Objects.requireNonNull(task.getException()).getMessage());
                }
            }
        });

        //Set Button Click Listeners
        optionOneBtn.setOnClickListener(this);
        optionTwoBtn.setOnClickListener(this);
        optionThreeBtn.setOnClickListener(this);
        nextBtn.setOnClickListener(this);
        closeBtn.setOnClickListener(this);

    }

    private void loadUI() {
        //Quiz Data Loaded, Load the UI
        quizTitle.setText(quizName);
        questionText.setText("Load First Question");

        //Enable Options
        enableOptions();

        //Load First Question
        loadQuestion(1);
    }

    private void loadQuestion(int questNum) {
        //Set Question Number
        questionNumber.setText(questNum + "");

        //Load Question Text
        questionText.setText(questionsToAnswer.get(questNum-1).getQuestion());

        //Load Options
        optionOneBtn.setText(questionsToAnswer.get(questNum-1).getOption_a());
        optionTwoBtn.setText(questionsToAnswer.get(questNum-1).getOption_b());
        optionThreeBtn.setText(questionsToAnswer.get(questNum-1).getOption_c());

        //Question Loaded, Set Can Answer
        canAnswer = true;
        currentQuestion = questNum;

        //Start Question Timer
        startTimer(questNum);
    }

    private void startTimer(int questionNumber) {

        //Set Timer Text
        final Long timeToAnswer = questionsToAnswer.get(questionNumber-1).getTimer();
        questionTime.setText(timeToAnswer.toString());

        //Show Timer ProgressBar
        questionProgress.setVisibility(View.VISIBLE);

        //Start CountDown
        //Update Time
        //Progress in percent
        //Time Up, Cannot Answer Question Anymore
        countDownTimer = new CountDownTimer(timeToAnswer * 1000, 10) {
            @Override
            public void onTick(long millisUntilFinished) {
                //Update Time
                questionTime.setText(millisUntilFinished / 1000 + "");

                //Progress in percent
                Long percent = millisUntilFinished / (timeToAnswer * 10);
                questionProgress.setProgress(percent.intValue());
            }

            @Override
            public void onFinish() {
                //Time Up, Cannot Answer Question Anymore
                canAnswer = false;

                questionFeedback.setText("Time Up! No Answer was submitted.");
                questionFeedback.setTextColor(getResources().getColor(R.color.colorPrimary,null));
                notAnswered++;
                showNextBtn();
            }
        };

        countDownTimer.start();
    }

    private void enableOptions() {
        //Show All Option Buttons
        optionOneBtn.setVisibility(View.VISIBLE);
        optionTwoBtn.setVisibility(View.VISIBLE);
        optionThreeBtn.setVisibility(View.VISIBLE);

        //Enable Option Buttons
        optionOneBtn.setEnabled(true);
        optionTwoBtn.setEnabled(true);
        optionThreeBtn.setEnabled(true);

        //Hide Feedback and next Button
        questionFeedback.setVisibility(View.INVISIBLE);
        nextBtn.setVisibility(View.INVISIBLE);
        nextBtn.setEnabled(false);
    }

    private void pickQuestions() {
        for(int i=0; i < totalQuestionsToAnswer; i++) {
            int randomNumber = getRandomInt(0, allQuestionsList.size());
            questionsToAnswer.add(allQuestionsList.get(randomNumber));
            allQuestionsList.removeAll(Collections.singleton(randomNumber));

            Log.d("QUESTIONS LOG", "Question " + i + " : " + questionsToAnswer.get(i).getQuestion());
        }
    }


    private int getRandomInt(int min, int max) {
        return ((int) (Math.random() * (max - min))) + min;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.quiz_close_btn:
                navController.navigate(R.id.action_quizFragment_to_listFragment);
                break;
            case R.id.quiz_option_one:
                verifyAnswer(optionOneBtn);
                break;
            case R.id.quiz_option_two:
                verifyAnswer(optionTwoBtn);
                break;
            case R.id.quiz_option_three:
                verifyAnswer(optionThreeBtn);
                break;
            case R.id.quiz_next_btn:
                if (currentQuestion == totalQuestionsToAnswer){
                    //Load Results
                    submitResults();
                }else {
                currentQuestion++;
                loadQuestion(currentQuestion);
                resetOptions();
                }
                break;
        }
    }

    private void submitResults() {
        HashMap<String, Object > resultMap = new HashMap<>();
        resultMap.put("correct",currentAnswers);
        resultMap.put("wrong",wrongAnswers);
        resultMap.put("unanswered",notAnswered);
        firebaseFirestore.collection("QuizList").document(quizId)
                .collection("Results").document(currentUserId).set(resultMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()){
                    //Go To Results Page
                    QuizFragmentDirections.ActionQuizFragmentToResultFragment action = QuizFragmentDirections.actionQuizFragmentToResultFragment();
                    action.setQuizId(quizId);
                    navController.navigate(action);
                }else {
                    //Show Error
                    quizTitle.setText(task.getException().getLocalizedMessage());
                }
            }
        });
    }


    private void verifyAnswer(Button selectedAnswerBtn) {
        //Set Answer Btn Text Color to black
        selectedAnswerBtn.setTextColor(getResources().getColor(R.color.colorDark,null));
        //Check Answer
        if(canAnswer){
            if(questionsToAnswer.get(currentQuestion-1).getAnswer().contentEquals(selectedAnswerBtn.getText())){
                //Correct Answer
                currentAnswers++;
                selectedAnswerBtn.setBackground(getResources().getDrawable(R.drawable.correct_answer_btn,null));

                //Set Feedback Text
                questionFeedback.setText("Correct Answer");
                questionFeedback.setTextColor(getResources().getColor(R.color.colorPrimary,null));
            } else {
                //Wrong Answer
                wrongAnswers++;
                selectedAnswerBtn.setBackground(getResources().getDrawable(R.drawable.wrong_answer_btn,null));

                //Set Feedback Text
                questionFeedback.setText("Wrong Answer \n Correct Answer : " + questionsToAnswer.get(currentQuestion-1).getAnswer());
                questionFeedback.setTextColor(getResources().getColor(R.color.colorAccent,null));
            }
            //Set Can answer to false
            canAnswer = false;

            //Stop The Timer
            countDownTimer.cancel();

            //Show Next Button
            showNextBtn();
        }
    }

    private void showNextBtn() {
        if (currentQuestion == totalQuestionsToAnswer){
            nextBtn.setText("Submit Results");
        }
        questionFeedback.setVisibility(View.VISIBLE);
        nextBtn.setVisibility(View.VISIBLE);
        nextBtn.setEnabled(true);
    }

    private void resetOptions() {
        optionOneBtn.setBackground(getResources().getDrawable(R.drawable.outline_light_btn_bg,null));
        optionTwoBtn.setBackground(getResources().getDrawable(R.drawable.outline_light_btn_bg,null));
        optionThreeBtn.setBackground(getResources().getDrawable(R.drawable.outline_light_btn_bg,null));

        optionOneBtn.setTextColor(getResources().getColor(R.color.colorLightText,null));
        optionTwoBtn.setTextColor(getResources().getColor(R.color.colorLightText,null));
        optionThreeBtn.setTextColor(getResources().getColor(R.color.colorLightText,null));

        questionFeedback.setVisibility(View.INVISIBLE);
        nextBtn.setVisibility(View.INVISIBLE);
        nextBtn.setEnabled(false);
    }
}
