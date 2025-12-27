package com.example.androidproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class PlacesActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_places);

        // Find all the city cards
        CardView islamabadCard = findViewById(R.id.islamabadCard);
        CardView karachiCard = findViewById(R.id.karachiCard);
        CardView lahoreCard = findViewById(R.id.lahoreCard);
        CardView faisalabadCard = findViewById(R.id.faisalabadCard);
        CardView kahutaCard = findViewById(R.id.kahutaCard);
        CardView quettaCard = findViewById(R.id.quettaCard);
        CardView rawalpindiCard = findViewById(R.id.rawalpindiCard);
        CardView kashmirCard = findViewById(R.id.kashmirCard);
        CardView naranCard = findViewById(R.id.naranCard);
        CardView babusarTopCard = findViewById(R.id.babusarTopCard);
        CardView peshawarCard = findViewById(R.id.peshawarCard);
        CardView murreeCard = findViewById(R.id.murreeCard);
        CardView khunzaCard = findViewById(R.id.khunzaCard);

        // Set click listeners for each card
        islamabadCard.setOnClickListener(this);
        karachiCard.setOnClickListener(this);
        lahoreCard.setOnClickListener(this);
        faisalabadCard.setOnClickListener(this);
        kahutaCard.setOnClickListener(this);
        quettaCard.setOnClickListener(this);
        rawalpindiCard.setOnClickListener(this);
        kashmirCard.setOnClickListener(this);
        naranCard.setOnClickListener(this);
        babusarTopCard.setOnClickListener(this);
        peshawarCard.setOnClickListener(this);
        murreeCard.setOnClickListener(this);
        khunzaCard.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        String cityName = "";
        int id = v.getId();
        if (id == R.id.islamabadCard) {
            cityName = "Islamabad";
        } else if (id == R.id.karachiCard) {
            cityName = "Karachi";
        } else if (id == R.id.lahoreCard) {
            cityName = "Lahore";
        } else if (id == R.id.faisalabadCard) {
            cityName = "Faisalabad";
        } else if (id == R.id.kahutaCard) {
            cityName = "Kahuta";
        } else if (id == R.id.quettaCard) {
            cityName = "Quetta";
        } else if (id == R.id.rawalpindiCard) {
            cityName = "Rawalpindi";
        } else if (id == R.id.kashmirCard) {
            cityName = "Kashmir";
        } else if (id == R.id.naranCard) {
            cityName = "Naran";
        } else if (id == R.id.babusarTopCard) {
            cityName = "Babusar Top";
        } else if (id == R.id.peshawarCard) {
            cityName = "Peshawar";
        } else if (id == R.id.murreeCard) {
            cityName = "Murree";
        } else if (id == R.id.khunzaCard) {
            cityName = "Khunza";
        }

        if (!cityName.isEmpty()) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("CITY_NAME", cityName);
            startActivity(intent);
        }
    }
}
