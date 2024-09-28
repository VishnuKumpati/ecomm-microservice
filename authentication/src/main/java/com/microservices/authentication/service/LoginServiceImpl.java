package com.microservices.authentication.service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.microservices.authentication.Repo.UserRepo;
import com.microservices.authentication.dto.LoginResponse;

@Service
public class LoginServiceImpl implements LoginService {

	@Autowired
	private EmailService emailService;

	@Autowired
	private UserRepo userDao;

	private Map<String, String> otpStore = new ConcurrentHashMap<>();
	private Map<String, Long> otpExpiry = new ConcurrentHashMap<>();
	private Map<String, Boolean> otpVerified = new ConcurrentHashMap<>(); // To store OTP verification status
	private static final long OTP_EXPIRY_TIME = 5 * 60 * 1000; // 5 minutes

	@Override
	public LoginResponse login(String email, String password) {
		if (email.equals("admin@zip.com") && password.equals("1234567890")) {
			return new LoginResponse(-1, email, password);
		}
		try {
			var user = userDao.findByEmail(email).orElseThrow();
			if (user.getPassword().equals(password)) {
				return new LoginResponse(user.getId(), user.getUsername(), user.getUserType());
			} else
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Incorrect password");
		} catch (Exception e) {
			e.printStackTrace();
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
		}
	}

	@Override
	public String updatePassword(String email, String password) {
		if (userDao.updatePassword(password, email) > 0) {
			return "Password updated successfully.";
		}
		return "Issue with password update.";
	}

	@Override
	public String existsByEmail(String email) {
		if (userDao.existsBymail(email)) {
			String otp = generateOtp();
			sendOtpEmail(email, otp);
			otpStore.put(email, otp);
			otpExpiry.put(email, System.currentTimeMillis() + OTP_EXPIRY_TIME);
			return "OTP sent to your email.";
		}
		return "Email not found.";
	}

	private String generateOtp() {
		Random random = new Random();
		return String.valueOf(100000 + random.nextInt(900000)); // 6-digit OTP
	}

	private void sendOtpEmail(String to, String otp) {
		emailService.sendEmail(to, "Your OTP Code", "Your OTP code is: " + otp);
	}
}