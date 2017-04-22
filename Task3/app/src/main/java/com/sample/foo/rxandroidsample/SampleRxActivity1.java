package com.sample.foo.rxandroidsample;

import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.sample.foo.rxandroidsample.databinding.ActivitySampleRx1Binding;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;

public class SampleRxActivity1 extends AppCompatActivity {

    ActivitySampleRx1Binding rx1Binding;
    Observable<String> myObservable;
    Observer<String> myObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Activity 1");
        createObservableAndObserver();

        rx1Binding = DataBindingUtil.setContentView(this, R.layout.activity_sample_rx1);

        rx1Binding.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myObservable.subscribe(myObserver);
            }
        });
    }

    private void createObservableAndObserver() {

        myObservable = Observable.create(
                new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> sub) {
                        Random r = new Random();
                        List<String> a = new ArrayList<>();

                        a.add("A good password has high entropy");
                        a.add("Common words are bad passwords");
                        a.add("A common password with a few letters substituted for numbers is also bad");
                        a.add("Passphrases should be fairly long - less than 3 words won't be enough");

                        sub.onNext(a.get(r.nextInt(a.size())));
                        sub.onCompleted();
                    }
                }
        );

        myObserver = new Observer<String>() {

            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(String text) {
                rx1Binding.textView.setText(text);
            }
        };

    }
}
