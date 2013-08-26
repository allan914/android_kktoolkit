/* Copyright (C) 2013 KKBOX Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * ​http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * @brief	KKDialogFactory is a factory class that helps create KKDialog.
 * 
 * @details
 * 
 * You may create one-button, two-button, three-button, or progress dialog.
 * The wording of the buttons is also custom.
 * You will need to implement KKDialogPostExecutionListener to handle click events.
 */
package com.kkbox.toolkit.dialog;


public class KKDialogFactory {

	/**
	 * @brief						This creates a one-button dialog.
	 * @param notificationId		A unique id for the notification.
	 * @param title					The title of the dialog.
	 * @param message				The message to be shown by the dialog.
	 * @param positiveButtonText	The text on the button of the dialog.
	 * @param listener				The corresponding KKDialogPostExecutionListener.
	 */
	public static KKDialog createAlertDialog(int notificationId, String title, String message, String positiveButtonText,
			KKDialogPostExecutionListener listener) {
		KKDialog dialog = new KKDialog();
		dialog.setContent(notificationId, title, message, positiveButtonText, "", "", KKDialog.Type.ALERT_DIALOG, listener);
		return dialog;
	}

	/**
	 * @brief						This creates a two-button dialog.
	 * @param notificationId		A unique id for the notification.
	 * @param title					The title of the dialog.
	 * @param message				The message to be shown by the dialog.
	 * @param positiveButtonText	The text on the right button of the dialog.
	 * @param negativeButtonText	The text on the left button of the dialog.
	 * @param listener				The corresponding KKDialogPostExecutionListener.
	 */
	public static KKDialog createYesOrNoDialog(int notificationId, String title, String message, String positiveButtonText, String negativeButtonText,
			KKDialogPostExecutionListener listener) {
		KKDialog dialog = new KKDialog();
		dialog.setContent(notificationId, title, message, positiveButtonText, negativeButtonText, "", KKDialog.Type.YES_OR_NO_DIALOG,
				listener);
		return dialog;
	}

	/**
	 * @brief						This creates a three-button dialog.
	 * @param notificationId		A unique id for the notification.
	 * @param title					The title of the dialog.
	 * @param message				The message to be shown by the dialog.
	 * @param positiveButtonText	The text on the right button of the dialog.
	 * @param neutralButtonText		The text on the middle button of the dialog.
	 * @param negativeButtonText	The text on the left button of the dialog.
	 * @param listener				The corresponding KKDialogPostExecutionListener.
	 */
	public static KKDialog createThreeChoiceDialog(int notificationId, String title, String message, String positiveButtonText, String neutralButtonText,
			String negativeButtonText, KKDialogPostExecutionListener listener) {
		KKDialog dialog = new KKDialog();
		dialog.setContent(notificationId, title, message, positiveButtonText, negativeButtonText, neutralButtonText,
				KKDialog.Type.THREE_CHOICE_DIALOG, listener);
		return dialog;
	}

	/**
	 * @brief						This creates a progress dialog.
	 * @param notificationId		A unique id for the notification.
	 * @param message				The message to be shown by the dialog.
	 * @param listener				The corresponding KKDialogPostExecutionListener.
	 */
	public static KKDialog createProgressingDialog(int notificationId, String message, KKDialogPostExecutionListener listener) {
		KKDialog dialog = new KKDialog();
		dialog.setContent(notificationId, "", message, "", "", "", KKDialog.Type.PROGRESSING_DIALOG, listener);
		return dialog;
	}
}
