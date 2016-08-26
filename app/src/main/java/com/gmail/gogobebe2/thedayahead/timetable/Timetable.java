package com.gmail.gogobebe2.thedayahead.timetable;

import android.content.pm.ActivityInfo;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;

import com.gmail.gogobebe2.thedayahead.R;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;

public class Timetable {
    private Week week;
    private TableLayout tableLayout;
    private static TimetableHighlighter timetableHighlighter;

    public Timetable(String htmlString, TimetableFragment timetableFragment) {
        this.tableLayout = (TableLayout) timetableFragment.getTimetableLinearLayout().findViewById(R.id.tablelayout_timetable);
        Document html = Jsoup.parse(htmlString);
        Element timetableTableElement = html.getElementsByTag("tbody").first();
        Elements rows = timetableTableElement.getElementsByTag("tr");


        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            Element row = rows.get(rowIndex);
            Elements columns = row.getElementsByTag("td");
            if (columns.isEmpty()) { // TOP ROW:
                Elements topRow = row.getElementsByTag("th");
                Element weekElement = topRow.remove(0);
                this.week = Week.parseWeek(weekElement, timetableFragment);

                for (Day day : Day.parseDays(topRow))
                    this.week.getDays().put(day,
                            new ArrayList<Period>());
            } else { // ALL LESSONS:
                columns.remove(0);
                for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {

                    // keySet() is still ordered because LinkedHashMap not HashMap.
                    Day day = (Day) week.getDays().keySet().toArray()[columnIndex];

                    Period period = null;

                    View view = tableLayout.findViewById(
                            tableLayout.getResources().getIdentifier(
                                    "lesson_r" + rowIndex + "c" + (columnIndex + 1), "id",
                                    tableLayout.getContext().getPackageName()));

                    if (view instanceof LinearLayout)
                        period = Lesson.parseLesson(columns.get(columnIndex), view, timetableFragment);
                    if (period == null) period = Period.parsePeriod(view, timetableFragment);

                    week.getDays().get(day).add(period);
                }
            }
        }
        
        if (timetableHighlighter == null) timetableHighlighter = new TimetableHighlighter(this);
        else timetableHighlighter.setTimetable(this);
        
        timetableHighlighter.execute();

        final RelativeLayout relativeLayout = timetableFragment.getLoginRelativeLayout();
        timetableFragment.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                relativeLayout.setVisibility(View.GONE);
                tableLayout.setVisibility(View.VISIBLE);
            }
        });
        timetableFragment.getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

    }

    public Week getWeek() {
        return this.week;
    }

    private Day getDay(int dayIndex) {
        int index = 0;
        for (Day day : getWeek().getDays().keySet()) {
            if (index == dayIndex) return day;
            index++;
        }
        return null;
    }

    private Period getPeriod(int periodIndex, int dayIndex) {
        return getWeek().getDays().get(getDay(dayIndex)).get(periodIndex);
    }

    public Period getPeriod(int dayInt, int hourInt, int minuteInt) {
        int dayIndex = dayInt - 2;

        if (dayIndex == 5 || dayIndex == -1) return null;

        Day day = getDay(dayIndex);
        if (day == null) throw new NullPointerException("day should never be null");

        int periodIndex;
        if (hourInt < 8 && minuteInt < 40) periodIndex = 0;
        else if (hourInt < 9) periodIndex = 1;
        else if (hourInt < 10) periodIndex = 2;
        else if (hourInt < 11) periodIndex = 3;
        else if (hourInt == 11 && minuteInt < 25) periodIndex = 4;
        else if (hourInt == 11 || (hourInt == 12 && minuteInt < 25)) periodIndex = 5;
        else if (hourInt == 12 || (hourInt == 13 && minuteInt < 25)) periodIndex = 6;
        else if (hourInt == 13 || (hourInt == 2 && minuteInt < 15)) periodIndex = 7;
        else if (hourInt == 14 || (hourInt == 3 && minuteInt < 15)) periodIndex = 8;
        else periodIndex = 9;

        return getPeriod(periodIndex, dayIndex);
    }
}
