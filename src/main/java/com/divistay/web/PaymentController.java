package com.divistay.web;

import com.divistay.domain.DomainException;
import com.divistay.payment.CardDetails;
import com.divistay.payment.PaymentResult;
import com.divistay.service.BookingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** A member's personal payment page. */
@Controller
public class PaymentController {

    private final BookingService booking;

    public PaymentController(BookingService booking) {
        this.booking = booking;
    }

    @GetMapping("/pay/{token}")
    public String page(@PathVariable String token, Model model) {
        return render(token, null, null, model);
    }

    @PostMapping("/pay/{token}")
    public String pay(@PathVariable String token,
                      @RequestParam(defaultValue = "") String holderName,
                      @RequestParam(defaultValue = "") String cardNumber,
                      @RequestParam(defaultValue = "0") int expiryMonth,
                      @RequestParam(defaultValue = "0") int expiryYear,
                      @RequestParam(defaultValue = "") String cvc,
                      Model model) {
        try {
            PaymentResult result = booking.pay(token, new CardDetails(holderName, cardNumber, expiryMonth, expiryYear, cvc));
            if (!result.approved()) {
                return render(token, result.declineReason(), holderName, model);
            }
            return "redirect:/pay/" + token + "?paid";
        } catch (DomainException e) {
            return render(token, e.getMessage(), holderName, model);
        }
    }

    private String render(String token, String error, String holderName, Model model) {
        BookingService.PaymentView view = booking.paymentView(token);
        model.addAttribute("token", token);
        model.addAttribute("view", view);
        model.addAttribute("error", error);
        model.addAttribute("holderName", holderName == null ? view.member().name() : holderName);
        return "pay";
    }
}
