package rocks.crownstone.dev_app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView

import android.view.KeyEvent
import android.widget.Toast

import kotlinx.android.synthetic.main.activity_login.*
import nl.komponents.kovenant.then
import nl.komponents.kovenant.ui.alwaysUi
import nl.komponents.kovenant.ui.failUi
import nl.komponents.kovenant.ui.successUi
import nl.komponents.kovenant.unwrap

/**
 * A login screen that offers login via email/password.
 */
class LoginActivity : AppCompatActivity() {
	/**
	 * Keep track of the login task to ensure we can cancel it if requested.
	 */
	private var _authTask: UserLoginTask? = null
	private val TAG = this.javaClass.simpleName

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		Log.i(TAG, "onCreate")
		setContentView(R.layout.activity_login)
		// Set up the login form.
		passwordText.setOnEditorActionListener(object: TextView.OnEditorActionListener {
			override fun onEditorAction(textView: TextView, id: Int, keyEvent: KeyEvent?): Boolean {
				if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
					attemptLogin()
					return true
				}
				return false
			}
		})

		email_sign_in_button.setOnClickListener { attemptLogin() }

	}

	/**
	 * Attempts to sign in or register the account specified by the login form.
	 * If there are form errors (invalid email, missing fields, etc.), the
	 * errors are presented and no actual login attempt is made.
	 */
	private fun attemptLogin() {
		if (_authTask != null) {
			return
		}

		// Reset errors.
		emailText.error = null
		passwordText.error = null

		// Store values at the time of the login attempt.
		val emailStr = emailText.text.toString()
		val passwordStr = passwordText.text.toString()

		var cancel = false
		var focusView: View? = null

		// Check for a valid password, if the user entered one.
		if (!TextUtils.isEmpty(passwordStr) && !isPasswordValid(passwordStr)) {
			passwordText.error = getString(R.string.error_invalid_password)
			focusView = passwordText
			cancel = true
		}

		// Check for a valid email address.
		if (TextUtils.isEmpty(emailStr)) {
			emailText.error = getString(R.string.error_field_required)
			focusView = emailText
			cancel = true
		}
		else if (!isEmailValid(emailStr)) {
			emailText.error = getString(R.string.error_invalid_email)
			focusView = emailText
			cancel = true
		}

		if (cancel) {
			// There was an error; don't attempt login and focus the first
			// form field with an error.
			focusView?.requestFocus()
		}
		else {
			// Show a progress spinner, and kick off a background task to
			// perform the user login attempt.
			showProgress(true)
			_authTask = UserLoginTask(emailStr, passwordStr)
//			_authTask!!.execute(null as Void?)
			_authTask!!.execute()
		}
	}

	private fun isEmailValid(email: String): Boolean {
		return email.contains("@")
	}

	private fun isPasswordValid(password: String): Boolean {
		return password.length > 0
	}

	/**
	 * Shows the progress UI and hides the login form.
	 */
	private fun showProgress(show: Boolean) {
//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			// Fade-in the progress spinner.
			val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

			login_form.visibility = if (show) View.GONE else View.VISIBLE
			login_form.animate()
					.setDuration(shortAnimTime)
					.alpha((if (show) 0 else 1).toFloat())
					.setListener(object : AnimatorListenerAdapter() {
						override fun onAnimationEnd(animation: Animator) {
							login_form.visibility = if (show) View.GONE else View.VISIBLE
						}
					})

			login_progress.visibility = if (show) View.VISIBLE else View.GONE
			login_progress.animate()
					.setDuration(shortAnimTime)
					.alpha((if (show) 1 else 0).toFloat())
					.setListener(object : AnimatorListenerAdapter() {
						override fun onAnimationEnd(animation: Animator) {
							login_progress.visibility = if (show) View.VISIBLE else View.GONE
						}
					})
//		}
//		else {
//			// The ViewPropertyAnimator APIs are not available, so simply show
//			// and hide the relevant UI components.
//			login_progress.visibility = if (show) View.VISIBLE else View.GONE
//			login_form.visibility = if (show) View.GONE else View.VISIBLE
//		}
	}

	inner class UserLoginTask internal constructor(email: String, password: String) {
		private val _email = email
		private val _password = password

		fun execute() {
			Log.i(TAG, "execute")
			MainApp.instance.user.login(_email, _password)
					.then {
						MainApp.instance.sphere.getSpheres(MainApp.instance.user)
					}.unwrap()
					.successUi {
//						this@LoginActivity.runOnUiThread { Toast.makeText(this@LoginActivity, "Response: %s".format(it), Toast.LENGTH_LONG).show() }
						Toast.makeText(this@LoginActivity, "Login successful", Toast.LENGTH_LONG).show()
						Log.i(TAG, MainApp.instance.sphere.toString())
					}
					.failUi {
						Toast.makeText(this@LoginActivity, "Error: %s".format(it.toString()), Toast.LENGTH_LONG).show()
						Log.e(TAG, "Error: ${it.message}")
						it.printStackTrace()
					}
					.alwaysUi {
						_authTask = null
						showProgress(false)
						Log.i(TAG, "done")
						setResult(Activity.RESULT_OK)
						finish()
					}
		}
	}
}
