package rocks.crownstone.dev_app.util

class CsError {
	class LoginException(override var message:String): Exception(message)
}