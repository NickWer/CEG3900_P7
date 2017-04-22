package com.ceg3900.nick.passcheck;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.jakewharton.threetenabp.AndroidThreeTen;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import me.gosimple.nbvcxz.Nbvcxz;
import me.gosimple.nbvcxz.resources.Feedback;
import me.gosimple.nbvcxz.scoring.Result;
import me.gosimple.nbvcxz.scoring.TimeEstimate;

public class MainActivity extends AppCompatActivity {
    TextView lblStrength = null;
    EditText txtPassword = null;
    Button btnGenerate, btnCrack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidThreeTen.init(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnCalculate = (Button)findViewById(R.id.btnCalculate);
        lblStrength = (TextView) findViewById(R.id.lblStrength);
        txtPassword = (EditText) findViewById(R.id.txtPassword);
        btnGenerate = (Button)findViewById(R.id.btnGenerate);
        btnCrack = (Button)findViewById(R.id.btnCrack);

        btnCrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                (new runSsh()).execute("ec2-52-26-55-136.us-west-2.compute.amazonaws.com", R.id.lblStrength, txtPassword.getText());
            }
        });

        (new MakeGenerator()).execute();

        btnCalculate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CalculatePassowrdStrength c = new CalculatePassowrdStrength();
                lblStrength.setText("Calculating... Please wait");
                c.execute(txtPassword.getText().toString());
            }
        });
    }

    /**
     * This method creates an passphrase listener with a wordlist behind it
     * @return ...
     */
    class MakeGenerator extends AsyncTask<Void, Void, Void> {
        List<String> wordlist = null;

        @Override
        protected Void doInBackground(Void... params) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open("words.txt")));
                wordlist = reader.lines().filter(word -> word.length() < 8).collect(Collectors.toList());
            } catch (IOException e) {
                e.printStackTrace();
            }

            List<String> finalWordlist = wordlist;
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            ((Button)findViewById(R.id.btnGenerate)).setOnClickListener(v -> {
                Random r = new Random();
                String output = "";
                for(int i = 0; i < 7; i++){
                    String word = wordlist.get(r.nextInt(wordlist.size()));
                    word = word.substring(0,1).toUpperCase() + word.substring(1);
                    output += word;
                }

                lblStrength.setText(output);
                txtPassword.setText(output);
            });
        }
    }
    class CalculatePassowrdStrength extends AsyncTask<String, Long, Result> {

        @Override
        protected Result doInBackground(String... params) {
            final String target = params[0].toLowerCase();
            Nbvcxz tester = new Nbvcxz();
            return tester.estimate(target);
        }

        @Override
        protected void onProgressUpdate(Long... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Result result) {
            String output = "Here's the feedback from the estimation session:\n";

            String timeToCrackOff = TimeEstimate.getTimeToCrackFormatted(result, "OFFLINE_BCRYPT_12");
            String timeToCrackOn = TimeEstimate.getTimeToCrackFormatted(result, "ONLINE_THROTTLED");

            output += "Overall your password is " + (result.isMinimumEntropyMet() ? "" : "not ") + "random enough\n\n";
            output += "Online time to crack: " + timeToCrackOn +"\n";
            output += "Offline time to crack: " + timeToCrackOff +"\n\n";
            Feedback feedback = result.getFeedback();
            if(feedback != null)
            {
                if (feedback.getWarning() != null)
                    output += "Warning: " + feedback.getWarning() + "\n";
                for (String suggestion : feedback.getSuggestion())
                {
                    output += "Suggestion: " + suggestion + "\n";
                }
            }
            lblStrength.setText(output);
        }
    }

    private class runSsh extends AsyncTask<Object, Void, Void> {
        String stdio = "Connected via ssh";
        TextView textfield = null;

        /**
         * Gets a File for the private key. Busts the space saving methods where
         * assets are meant to be read from the apk directly.
         *
         * http://stackoverflow.com/questions/8474821
         * @return a File object referencing the private key
         */
        private File getPrivKey(){
            File f = new File(getCacheDir()+"/hcapp.pem");
            if (!f.exists()) try {

                InputStream is = getAssets().open("hcapp.pem");
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();


                FileOutputStream fos = new FileOutputStream(f);
                fos.write(buffer);
                fos.close();
            } catch (Exception e) { throw new RuntimeException(e); }

            return f;
        }

        private String buildCommand(String server){
            String out = "ssh -i ";
            out += getPrivKey().getPath() + " ";
            out += "ubuntu@" + server;
            return out;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            textfield.setText(stdio);
        }

        @Override
        protected Void doInBackground(Object... params) {
            Process p = null;
            try {
                p = Runtime.getRuntime().exec(buildCommand((String)params[0]));
                PrintStream out = new PrintStream(p.getOutputStream());
                BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                textfield = (TextView) findViewById((int)params[1]);

                out.println("echo -n \"" + params[2] + "\" | ./hashcat64.bin -m 0 -a 0 --show words.txt");
                while (in.ready()) {
                    stdio += "\n" + in.readLine();
                    publishProgress();
                }
                out.println("exit");

                p.waitFor();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
