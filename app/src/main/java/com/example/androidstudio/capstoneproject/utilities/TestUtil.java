package com.example.androidstudio.capstoneproject.utilities;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.example.androidstudio.capstoneproject.data.LessonsContract;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * This class inserts fake data in the local database for testing.
 */
public class TestUtil {

    private static final String TAG = TestUtil.class.getSimpleName();


    public static void insertFakeData(Context context){


        String[] partsText = new String[]{
                "Part 1\n\n" +
                        "Synergistically cultivate e-business markets without client-centric solutions. Conveniently" +
                        " build front-end outsourcing with clicks-and-mortar infrastructures. Credibly" +
                        " myocardinate orthogonal scenarios without synergistic data. Conveniently initiate " +
                        "equity invested ROI for global methodologies. Seamlessly exploit multifunctional" +
                        " potentialities and innovative markets.\n" +
                        "\n" +
                        "Interactively formulate plug-and-play resources before global web services. " +
                        "Authoritatively myocardinate process-centric leadership with resource-leveling " +
                        "imperatives. Progressively restore virtual solutions vis-a-vis B2B growth strategies. " +
                        "Professionally predominate covalent solutions for magnetic infrastructures. " +
                        "Enthusiastically scale prospective resources with client-centric expertise.\n" +
                        "\n" +
                        "Competently myocardinate cross-media value for process-centric technologies. " +
                        "Distinctively actualize front-end imperatives through worldwide metrics. Professionally " +
                        "engineer accurate process improvements rather than standardized e-services. " +
                        "Globally target orthogonal value after high-payoff e-tailers. Energistically " +
                        "disintermediate global alignments and cross-media applications.\n" +
                        "\n" +
                        "Authoritatively recaptiualize holistic alignments before enterprise-wide strategic " +
                        "theme areas. Efficiently unleash user friendly manufactured products before " +
                        "out-of-the-box applications. Continually expedite real-time vortals vis-a-vis " +
                        "open-source schemas. Dynamically communicate 2.0 supply chains rather than " +
                        "enabled e-tailers. Holisticly transform client-centered best practices with " +
                        "error-free web-readiness.\n" +
                        "\n" +
                        "Rapidiously productize.",

                "Part 2\n\n"+
                        "Synergistically synergize 2.0 catalysts for change rather than extensible quality vectors. " +
                        "Conveniently enhance bleeding-edge vortals without orthogonal processes. Globally " +
                        "empower progressive materials after performance based content. Holisticly promote " +
                        "cross-unit human capital without high-quality potentialities. Globally customize " +
                        "user-centric vortals vis-a-vis orthogonal innovation.\n" +
                        "\n" +
                        "Intrinsicly iterate efficient supply chains whereas value-added opportunities. " +
                        "Proactively deliver tactical sources vis-a-vis granular functionalities. Conveniently " +
                        "fashion multifunctional niche markets vis-a-vis business deliverables. Globally " +
                        "reconceptualize leveraged architectures rather than equity invested materials. " +
                        "Efficiently reinvent end-to-end technologies through team building e-markets.\n" +
                        "\n" +
                        "Rapidiously repurpose focused expertise through intuitive data. Dynamically " +
                        "myocardinate timely ROI after impactful innovation. Dynamically fabricate e-business " +
                        "applications and real-time technologies. Dynamically aggregate exceptional " +
                        "potentialities rather than bricks-and-clicks infomediaries. Globally maximize " +
                        "out-of-the-box technologies for revolutionary ideas.\n" +
                        "\n" +
                        "Interactively enable leveraged best practices whereas an expanded array of users. " +
                        "Collaboratively create tactical products after functionalized data. Compellingly " +
                        "underwhelm principle-centered web-readiness and cross-media best practices. " +
                        "Interactively whiteboard high-quality channels before client-focused infomediaries. " +
                        "Synergistically predominate exceptional users.",

                "Part 3\n\n" +
                        "Dramatically build low-risk high-yield best practices through best-of-breed technology. " +
                        "Collaboratively unleash diverse leadership skills rather than vertical collaboration " +
                        "and idea-sharing. Holisticly initiate synergistic platforms after seamless collaboration " +
                        "and idea-sharing. Dynamically streamline competitive functionalities before " +
                        "cross-platform ROI. Competently actualize exceptional methodologies whereas enabled " +
                        "markets.\n" +
                        "\n" +
                        "Conveniently actualize intuitive opportunities with an expanded array of e-business. " +
                        "Conveniently coordinate exceptional methodologies via premier e-markets. Holisticly " +
                        "seize parallel technologies rather than user-centric channels. Seamlessly reinvent " +
                        "scalable innovation whereas goal-oriented infomediaries. Globally negotiate value-added " +
                        "ROI through emerging vortals.\n" +
                        "\n" +
                        "Dramatically provide access to standardized methodologies rather than state of the " +
                        "art technology. Quickly pursue visionary outsourcing without orthogonal models. " +
                        "Interactively iterate high standards in core competencies before global models. " +
                        "Competently pursue enterprise-wide technology vis-a-vis quality models. Seamlessly " +
                        "facilitate scalable partnerships rather than client-focused action items.\n" +
                        "\n" +
                        "Completely underwhelm worldwide resources without excellent process improvements. " +
                        "Seamlessly optimize principle-centered supply chains and highly efficient communities. " +
                        "Phosfluorescently e-enable out-of-the-box infrastructures with functional process " +
                        "improvements. Rapidiously.",

                "Part 4\n\n" +
                        "Compellingly fashion corporate markets vis-a-vis multidisciplinary products. Synergistically " +
                        "re-engineer strategic innovation with enabled mindshare. Phosfluorescently expedite " +
                        "synergistic scenarios rather than end-to-end users. Assertively impact progressive " +
                        "growth strategies after interoperable communities. Compellingly seize functionalized " +
                        "deliverables for end-to-end leadership.\n" +
                        "\n" +
                        "Uniquely simplify intermandated leadership before functional opportunities. " +
                        "Conveniently network scalable scenarios vis-a-vis quality models. Conveniently " +
                        "expedite holistic total linkage for flexible innovation. Energistically disseminate " +
                        "high-payoff alignments after frictionless methods of empowerment. Credibly architect " +
                        "standardized value rather than robust functionalities.\n" +
                        "\n" +
                        "Energistically pursue ethical supply chains after value-added manufactured products. " +
                        "Rapidiously maintain cutting-edge strategic theme areas without future-proof data. " +
                        "Continually redefine professional deliverables with ethical processes. Enthusiastically " +
                        "extend stand-alone platforms after high-quality core competencies. Competently " +
                        "maximize premium interfaces through exceptional channels.\n" +
                        "\n" +
                        "Synergistically coordinate focused bandwidth for flexible markets. Synergistically " +
                        "visualize parallel e-services and equity invested core competencies. Enthusiastically " +
                        "innovate reliable total linkage for interactive manufactured products. Competently " +
                        "streamline clicks-and-mortar communities before vertical information. Distinctively " +
                        "procrastinate sticky supply chains whereas.",

                "Part 5\n\n" +
                        "Monotonectally conceptualize visionary e-markets vis-a-vis corporate architectures. " +
                        "Synergistically e-enable focused relationships and maintainable applications. " +
                        "Conveniently pursue alternative communities and ethical paradigms. Synergistically " +
                        "scale viral innovation whereas leading-edge web services. Seamlessly leverage " +
                        "other's cross-media methodologies rather than impactful infomediaries.\n" +
                        "\n" +
                        "Competently generate exceptional niche markets rather than robust process improvements. " +
                        "Competently iterate cross-unit ideas for B2C communities. Holisticly visualize " +
                        "impactful models before prospective e-tailers. Synergistically morph highly efficient " +
                        "interfaces through world-class methodologies. Monotonectally create bleeding-edge " +
                        "quality vectors vis-a-vis low-risk high-yield alignments.\n" +
                        "\n" +
                        "Holisticly brand transparent interfaces without impactful synergy. Competently " +
                        "deploy maintainable e-tailers rather than maintainable systems. Holisticly transition " +
                        "wireless processes for user friendly paradigms. Proactively procrastinate user " +
                        "friendly methods of empowerment and client-focused intellectual capital. Appropriately " +
                        "maintain enterprise solutions and efficient imperatives.\n" +
                        "\n" +
                        "Enthusiastically whiteboard cross-media web services whereas fully researched ideas. " +
                        "Credibly synergize empowered \"outside the box\" thinking and world-class materials. " +
                        "Conveniently administrate dynamic schemas for plug-and-play technology. Appropriately " +
                        "e-enable fully researched customer service before stand-alone.",

                "Part 6\n\n" +
                        "Efficiently visualize backend leadership for resource-leveling deliverables. Globally " +
                        "disseminate premier quality vectors through high-payoff methodologies. Phosfluorescently " +
                        "implement one-to-one catalysts for change for performance based content. Collaboratively " +
                        "disseminate integrated initiatives and dynamic human capital. Enthusiastically " +
                        "target enabled applications without wireless relationships.\n" +
                        "\n" +
                        "Monotonectally provide access to leading-edge scenarios whereas scalable outsourcing. " +
                        "Synergistically predominate premier expertise whereas visionary platforms. " +
                        "Assertively conceptualize prospective experiences after collaborative internal " +
                        "or \"organic\" sources. Objectively re-engineer team driven users after e-business " +
                        "materials. Seamlessly maintain highly efficient products after vertical models.\n" +
                        "\n" +
                        "Completely negotiate excellent mindshare without customized benefits. Distinctively " +
                        "provide access to B2B results and cooperative architectures. Competently synthesize " +
                        "web-enabled meta-services and sustainable e-commerce. Monotonectally target end-to-end " +
                        "systems for functional testing procedures. Globally incentivize ubiquitous technology " +
                        "with state of the art manufactured products.\n" +
                        "\n" +
                        "Quickly morph impactful collaboration and idea-sharing with turnkey information. " +
                        "Credibly supply distributed paradigms whereas impactful innovation. Collaboratively " +
                        "e-enable sticky scenarios for installed base networks. Completely visualize visionary " +
                        "portals without clicks-and-mortar.\n" +
                        "\n\n" +
                        "Generated by Corporate Ipsum extension for Chrome offered by BilliamTheSecond.\n" +
                        "<https://chrome.google.com/webstore/detail/corporate-ipsum/>\n\n"

        };

        String fromGroup = "\n(From Group)";


        ContentResolver contentResolver = context.getContentResolver();

        // create for the local users tables
        for (int i = 1; i <= 6; i++) {

            ContentValues testLessonValues = new ContentValues();
            testLessonValues.put(LessonsContract.MyLessonsEntry.COLUMN_LESSON_TITLE, "Lesson " + i +
            " author: myself");

            Uri lessonUri = contentResolver.insert(LessonsContract.MyLessonsEntry.CONTENT_URI,
                    testLessonValues);

            if (lessonUri != null) {
                Log.d(TAG, "insertFakeData lessonUri:" + lessonUri.toString());
            } else {
                Log.e(TAG, "insertFakeData lessonUri error!");
            }

            Cursor cursor = contentResolver.query(LessonsContract.MyLessonsEntry.CONTENT_URI,
                    null,
                    null,
                    null,
                    null);

            Long lesson_id = (long) -1;

            if(null != cursor) {
                cursor.moveToLast();
                int columnIndex = cursor.getColumnIndex(LessonsContract.MyLessonsEntry._ID);
                lesson_id = cursor.getLong(columnIndex);
                cursor.close();
            }

            if (!(lesson_id > 0)) {
                Log.e(TAG, "insertFakeData cursor error!");
            }

            for (int j = 1; j <= 6; j++) {

                ContentValues testLessonPartsValues = new ContentValues();
                testLessonPartsValues.put(LessonsContract.MyLessonPartsEntry.COLUMN_LESSON_ID, lesson_id);
                testLessonPartsValues.put(LessonsContract.MyLessonPartsEntry.COLUMN_PART_TITLE,
                        " Lesson " + i + "  -  Part " + j + " author: myself");
                testLessonPartsValues.put(LessonsContract.MyLessonPartsEntry.COLUMN_PART_TEXT,
                        partsText[j-1]);

                Uri partUri = contentResolver.insert(LessonsContract.MyLessonPartsEntry.CONTENT_URI,
                        testLessonPartsValues);

                if (partUri != null) {
                    Log.d(TAG, "insertFakeData partUri:" + partUri.toString());
                } else {
                    Log.e(TAG, "insertFakeData partUri error!");
                }

            }
        }


        long part_id_author1 = 1;

        // create for the group users tables
        for (int lesson_id = 1; lesson_id <= 3; lesson_id++) {

            ContentValues testLessonValues = new ContentValues();
            testLessonValues.put(LessonsContract.GroupLessonsEntry.COLUMN_LESSON_ID, lesson_id);
            testLessonValues.put(LessonsContract.GroupLessonsEntry.COLUMN_LESSON_TITLE, "Lesson "
                    + lesson_id + " author: ABCDEF");
            String time_stamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US)
                    .format(new Date());
            testLessonValues.put(LessonsContract.GroupLessonsEntry.COLUMN_LESSON_TIME_STAMP, time_stamp);
            testLessonValues.put(LessonsContract.GroupLessonsEntry.COLUMN_USER_UID, "ABCDEF");

            Uri lessonUri = contentResolver.insert(LessonsContract.GroupLessonsEntry.CONTENT_URI,
                    testLessonValues);

            long inserted_lesson_id = -1;

            if (lessonUri != null) {
                inserted_lesson_id = Long.parseLong(lessonUri.getPathSegments().get(1));
                Log.d(TAG, "insertFakeData lessonUri:" + lessonUri.toString());
                Log.d(TAG, "insertFakeData inserted_lesson_id:" + inserted_lesson_id);
            } else {
                Log.e(TAG, "insertFakeData lessonUri error!");
            }

            if (inserted_lesson_id == -1) {
                Log.e(TAG, "insertFakeData inserted_lesson_id error!");
                return;
            }

            for (int j = 1; j <= 6; j++) {

                ContentValues testLessonPartsValues = new ContentValues();
                testLessonPartsValues.put(LessonsContract.GroupLessonPartsEntry.COLUMN_LESSON_ID,
                        Long.toString(inserted_lesson_id));
                testLessonPartsValues.put(LessonsContract.GroupLessonPartsEntry.COLUMN_PART_ID, part_id_author1);
                testLessonPartsValues.put(LessonsContract.GroupLessonPartsEntry.COLUMN_PART_TITLE,
                        " Lesson " + lesson_id + "  -  Part " + j + " author: ABCDEF");
                testLessonPartsValues.put(LessonsContract.MyLessonPartsEntry.COLUMN_PART_TEXT,
                        partsText[j-1] + fromGroup);
                testLessonPartsValues.put(LessonsContract.GroupLessonPartsEntry.COLUMN_USER_UID, "ABCDEF");

                Uri partUri = contentResolver.insert(LessonsContract.GroupLessonPartsEntry.CONTENT_URI,
                        testLessonPartsValues);

                if (partUri != null) {
                    Log.d(TAG, "insertFakeData partUri:" + partUri.toString());
                } else {
                    Log.e(TAG, "insertFakeData partUri error!");
                }

                part_id_author1++;

            }
        }


        long part_id_author2 = 1;

        // create for the group users tables
        for (int lesson_id = 1; lesson_id <= 3; lesson_id++) {

            // insert one lesson
            ContentValues testLessonValues = new ContentValues();
            // lesson id as in the external database
            testLessonValues.put(LessonsContract.GroupLessonsEntry.COLUMN_LESSON_ID, lesson_id);
            testLessonValues.put(LessonsContract.GroupLessonsEntry.COLUMN_LESSON_TITLE, "Lesson "
                    + lesson_id + " author: GHIJKL");
            String time_stamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US)
                    .format(new Date());
            testLessonValues.put(LessonsContract.GroupLessonsEntry.COLUMN_LESSON_TIME_STAMP, time_stamp);
            testLessonValues.put(LessonsContract.GroupLessonsEntry.COLUMN_USER_UID, "GHIJKL");

            Uri lessonUri = contentResolver.insert(LessonsContract.GroupLessonsEntry.CONTENT_URI,
                    testLessonValues);

            long inserted_lesson_id = -1;

            if (lessonUri != null) {
                inserted_lesson_id = Long.parseLong(lessonUri.getPathSegments().get(1));
                Log.d(TAG, "insertFakeData lessonUri:" + lessonUri.toString());
                Log.d(TAG, "insertFakeData inserted_lesson_id:" + inserted_lesson_id);
            } else {
                Log.e(TAG, "insertFakeData lessonUri error!");
            }

            if (inserted_lesson_id == -1) {
                Log.e(TAG, "insertFakeData inserted_lesson_id error!");
                return;
            }

            // insert six parts
            for (int j = 1; j <= 6; j++) {

                ContentValues testLessonPartsValues = new ContentValues();
                // _id of the lesson
                testLessonPartsValues.put(LessonsContract.GroupLessonPartsEntry.COLUMN_LESSON_ID,
                        Long.toString(inserted_lesson_id));
                // part id as in the external database
                testLessonPartsValues.put(LessonsContract.GroupLessonPartsEntry.COLUMN_PART_ID, part_id_author2);
                testLessonPartsValues.put(LessonsContract.GroupLessonPartsEntry.COLUMN_PART_TITLE,
                        " Lesson " + lesson_id + "  -  Part " + j + " author: GHIJKL");
                testLessonPartsValues.put(LessonsContract.MyLessonPartsEntry.COLUMN_PART_TEXT,
                        partsText[j-1] + fromGroup);
                testLessonPartsValues.put(LessonsContract.GroupLessonPartsEntry.COLUMN_USER_UID, "GHIJKL");

                Uri partUri = contentResolver.insert(LessonsContract.GroupLessonPartsEntry.CONTENT_URI,
                        testLessonPartsValues);

                if (partUri != null) {
                    Log.d(TAG, "insertFakeData partUri:" + partUri.toString());
                } else {
                    Log.e(TAG, "insertFakeData partUri error!");
                }

                part_id_author2++;

            }
        }

    }

}