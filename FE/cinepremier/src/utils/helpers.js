export const formatCurrency = (value) => `${Number(value || 0).toLocaleString('vi-VN')}đ`;

export const formatDate = (value, options = {}) => {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  return date.toLocaleDateString('vi-VN', options);
};
