import * as momentLib from 'moment'

export const formats = {
  DATE_EXTENDED: 'MMM DD, YYYY',
  DATETIME_EXTENDED: 'MMM DD, YYYY, HH:mm:ssZ',
  DATETIME_EXTENDED_WITHOUT_SECONDS: 'MMM DD, YYYY, HH:mm',
  ISO_DATE_ONLY: 'YYYY-MM-DD',
  ISO_DATETIME: 'YYYY-MM-DDTHH:mm:ssZ'
}

export function formatDateToExtended (dateInMoment) {
  return dateInMoment.format(formats.DATE_EXTENDED)
}

export function formatDatetimeToExtended (dateInMoment) {
  return dateInMoment.format(formats.DATETIME_EXTENDED)
}

export function formatToExtended (dateInMoment, format = formats.DATETIME_EXTENDED) {
  return dateInMoment.format(format)
}

export const moment = momentLib
