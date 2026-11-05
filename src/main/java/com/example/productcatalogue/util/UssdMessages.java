package com.example.productcatalogue.util;

import com.example.productcatalogue.model.Language;

import java.util.EnumMap;
import java.util.Map;

public final class UssdMessages {

    private UssdMessages() {}

    public enum Key {
        DIAL_HINT,
        LANGUAGE_SELECTION,
        ENGLISH,
        KINYARWANDA,
        NEXT,
        BACK,
        EXIT,
        NO_CATEGORIES,
        NO_PRODUCTS,
        INVALID_SELECTION,
        RESUME_HEADER,
        RESUME_CONTINUE,
        RESUME_START_NEW,
        EXIT_THANKS,
        LABEL_CATEGORY,
        LABEL_PRICE,
        LABEL_VALIDITY,
        DAYS,
        CHANGE_LANGUAGE,
        RETURN_BACK,
        VALIDITY_NO_EXPIRY,
        PAY_VIA_HEADER,
        OPTION_AIRTIME,
        OPTION_MOMO,
        OPTION_IHEREZE,
        GO_BACK,
        ENTER_MOMO_PIN,
        INSUFFICIENT_BALANCE_HEADER,
        LOAN_CONFIRM_HEADER,
        CONFIRM,
        CANCEL,
        PURCHASE_CANCELLED,
        PAID_VIA_AIRTIME,
        PAID_VIA_MOMO,
        LOAN_APPROVED_NOTE
    }

    private static final Map<Language, Map<Key, String>> MESSAGES = new EnumMap<>(Language.class);

    static {
        Map<Key, String> en = new EnumMap<>(Key.class);
        en.put(Key.DIAL_HINT, "Dial *100# for great deals!");
        en.put(Key.LANGUAGE_SELECTION, "Select language:");
        en.put(Key.ENGLISH, "English");
        en.put(Key.KINYARWANDA, "Kinyarwanda");
        en.put(Key.NEXT, "Next");
        en.put(Key.BACK, "Back");
        en.put(Key.EXIT, "Exit");
        en.put(Key.NO_CATEGORIES, "No categories available at the moment.");
        en.put(Key.NO_PRODUCTS, "No products available in this category.");
        en.put(Key.INVALID_SELECTION, "Invalid selection. Please dial %s again.");
        en.put(Key.RESUME_HEADER, "You have an unfinished session%s.");
        en.put(Key.RESUME_CONTINUE, "Continue where you left off");
        en.put(Key.RESUME_START_NEW, "Start a new session");
        en.put(Key.EXIT_THANKS, "Thank you for using our Product Catalogue service.");
        en.put(Key.LABEL_CATEGORY, "Category");
        en.put(Key.LABEL_PRICE, "Price");
        en.put(Key.LABEL_VALIDITY, "Validity");
        en.put(Key.DAYS, "day(s)");
        en.put(Key.CHANGE_LANGUAGE, "Change language");
        en.put(Key.RETURN_BACK, "Return Back");
        en.put(Key.VALIDITY_NO_EXPIRY, "Until fully used");
        en.put(Key.PAY_VIA_HEADER, "Pay %sFrw = %s via:");
        en.put(Key.OPTION_AIRTIME, "Airtime");
        en.put(Key.OPTION_MOMO, "Momo");
        en.put(Key.OPTION_IHEREZE, "Ihereze");
        en.put(Key.GO_BACK, "Go back");
        en.put(Key.ENTER_MOMO_PIN, "Enter your Momo PIN:");
        en.put(Key.INSUFFICIENT_BALANCE_HEADER,
                "You have insufficient balance, confirm to get a loan of %sFrw with %sFrw service fee. You will repay %sFrw on your next recharge:");
        en.put(Key.LOAN_CONFIRM_HEADER,
                "Confirm to get a loan of %sFrw with %sFrw service fee. You will repay %sFrw on your next recharge:");
        en.put(Key.CONFIRM, "Confirm");
        en.put(Key.CANCEL, "Cancel");
        en.put(Key.PURCHASE_CANCELLED, "Purchase cancelled.");
        en.put(Key.PAID_VIA_AIRTIME, "Paid via Airtime");
        en.put(Key.PAID_VIA_MOMO, "Paid via Momo");
        en.put(Key.LOAN_APPROVED_NOTE, "Loan of %sFrw approved via Ihereze. Repay %sFrw on your next recharge.");
        MESSAGES.put(Language.EN, en);

        Map<Key, String> rw = new EnumMap<>(Key.class);
        rw.put(Key.DIAL_HINT, "Hamagara *100# uhabwe ibiciro byiza!");
        rw.put(Key.LANGUAGE_SELECTION, "Hitamo ururimi:");
        rw.put(Key.ENGLISH, "Icyongereza");
        rw.put(Key.KINYARWANDA, "Kinyarwanda");
        rw.put(Key.NEXT, "Ibikurikira");
        rw.put(Key.BACK, "Subira inyuma");
        rw.put(Key.EXIT, "Gusohoka");
        rw.put(Key.NO_CATEGORIES, "Nta byiciro bihari kuri ubu.");
        rw.put(Key.NO_PRODUCTS, "Nta bicuruzwa bihari muri iki cyiciro.");
        rw.put(Key.INVALID_SELECTION, "Amahitamo atemewe. Ongera uhamagare %s.");
        rw.put(Key.RESUME_HEADER, "Ufite igihe cyahagaritswe%s.");
        rw.put(Key.RESUME_CONTINUE, "Komeza aho wari ugeze");
        rw.put(Key.RESUME_START_NEW, "Tangira igihe gishya");
        rw.put(Key.EXIT_THANKS, "Murakoze gukoresha serivisi yacu y'ibicuruzwa.");
        rw.put(Key.LABEL_CATEGORY, "Icyiciro");
        rw.put(Key.LABEL_PRICE, "Igiciro");
        rw.put(Key.LABEL_VALIDITY, "Igihe gifatika");
        rw.put(Key.DAYS, "iminsi");
        rw.put(Key.CHANGE_LANGUAGE, "Hindura ururimi");
        rw.put(Key.RETURN_BACK, "Subira Inyuma");
        rw.put(Key.VALIDITY_NO_EXPIRY, "Kugeza irangiye");
        rw.put(Key.PAY_VIA_HEADER, "Ishyura %sFrw = %s uciye kuri:");
        rw.put(Key.OPTION_AIRTIME, "Igihimbano (Airtime)");
        rw.put(Key.OPTION_MOMO, "Momo");
        rw.put(Key.OPTION_IHEREZE, "Ihereze");
        rw.put(Key.GO_BACK, "Subira inyuma");
        rw.put(Key.ENTER_MOMO_PIN, "Andika ijambo ry'ibanga (PIN) rya Momo:");
        rw.put(Key.INSUFFICIENT_BALANCE_HEADER,
                "Ntabwo ufite amafaranga ahagije, emeza kugira ngo ubone inguzanyo ya %sFrw hamwe n'amafaranga y'uburyozwe %sFrw. Uzasubiza %sFrw igihe uzongera kwishyuza:");
        rw.put(Key.LOAN_CONFIRM_HEADER,
                "Emeza kugira ngo ubone inguzanyo ya %sFrw hamwe n'amafaranga y'uburyozwe %sFrw. Uzasubiza %sFrw igihe uzongera kwishyuza:");
        rw.put(Key.CONFIRM, "Emeza");
        rw.put(Key.CANCEL, "Reka");
        rw.put(Key.PURCHASE_CANCELLED, "Kugura byahagaritswe.");
        rw.put(Key.PAID_VIA_AIRTIME, "Byishyuwe uciye kuri Airtime");
        rw.put(Key.PAID_VIA_MOMO, "Byishyuwe uciye kuri Momo");
        rw.put(Key.LOAN_APPROVED_NOTE, "Inguzanyo ya %sFrw yemejwe uciye kuri Ihereze. Uzasubiza %sFrw igihe uzongera kwishyuza.");
        MESSAGES.put(Language.RW, rw);
    }

    public static String get(Language language, Key key) {
        return MESSAGES.get(language != null ? language : Language.EN).get(key);
    }

    public static String get(Language language, Key key, Object... args) {
        return String.format(get(language, key), args);
    }
}
