export const MAX_NAME_LENGTH = 100;
export const PHONE_PREFIXES = ['03', '05', '08', '09'];
export const VIETNAM_PHONE_REGEX = /^(03|05|08|09)\d{8}$/;
export const STRONG_PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/;

export const normalizePhoneInput = (value) => value.replace(/\D/g, '').slice(0, 10);

export const normalizeNameInput = (value) => value.slice(0, MAX_NAME_LENGTH);

export const isValidVietnamPhone = (value) => VIETNAM_PHONE_REGEX.test(value.trim());
export const isStrongPassword = (value) => STRONG_PASSWORD_REGEX.test(value);

export const PHONE_VALIDATION_MESSAGE = 'Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 03, 05, 08 hoặc 09.';
export const NAME_VALIDATION_MESSAGE = `Tên không được vượt quá ${MAX_NAME_LENGTH} ký tự.`;
export const PASSWORD_VALIDATION_MESSAGE = 'Mật khẩu phải có tối thiểu 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt.';
