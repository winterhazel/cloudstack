import moment from 'moment'

export function exportDataToCsv ({ data = null, keys = null, columnDelimiter = ',', lineDelimiter = '\n', fileName = 'data', dateFormat = undefined }) {
  if (data === null || !data.length || keys === null || !keys.filter(key => key !== null && key !== '').length) {
    return null
  }

  let dataParsed = ''
  dataParsed += keys.join(columnDelimiter)
  dataParsed += lineDelimiter

  data.forEach(item => {
    keys.forEach(key => {
      if (item[key] === undefined) {
        item[key] = ''
      }

      if (typeof item[key] === 'string' && item[key].includes(columnDelimiter)) {
        dataParsed += `"${item[key]}"`
      } else if (dateFormat && item[key] instanceof moment) {
        dataParsed += `"${item[key].format(dateFormat)}"`
      } else {
        dataParsed += item[key]
      }

      dataParsed += columnDelimiter
    })
    dataParsed = dataParsed.slice(0, -1)
    dataParsed += lineDelimiter
  })

  const hiddenElement = document.createElement('a')
  hiddenElement.href = 'data:text/csv;charset=utf-8,' + encodeURI(dataParsed)
  hiddenElement.target = '_blank'
  hiddenElement.download = `${fileName}.csv`
  hiddenElement.click()
  hiddenElement.remove()
}
