package elegit.controllers;


import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class LogTest {

    public static void main(String[] args) {

        Logger console = LogManager.getLogger("briefconsolelogger");
        System.out.println("From println");
        console.info("Hi");

        Observable.fromCallable(() -> {
            console.info("wow");
            System.out.println("here");
            return true;
        })
                .subscribeOn(Schedulers.io())
                .doOnNext(unused -> {
                    console.info("next");
                })
                .subscribe();

        Single.fromCallable(() -> {
            console.info("single wow");
            return true;
        })
                .doOnSuccess(unused -> {
                    console.info("single next");
                }).subscribe();

    }
}
