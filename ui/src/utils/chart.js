import { TIME_UNITS } from './units'

export const defaultChartOptions = {
  maintainAspectRatio: false
}

export const getUnitToTimeCartesianAxis = (baseUnit, dataLength) => {
  const maxLabels = 15
  if (dataLength <= maxLabels) {
    return baseUnit
  }

  const units = [
    'millisecond',
    'second',
    'minute',
    'hour',
    'day',
    'week',
    'month',
    'quarter',
    'year'
  ]

  let index = units.indexOf(baseUnit)

  let unitToReturn = baseUnit
  if (index >= 0 && index < units.length) {
    let unitTime = 0
    for (index; index < units.length; index++) {
      unitTime = TIME_UNITS[units[index]]
      const nextUnitTime = TIME_UNITS[units[index + 1]]

      if ((dataLength / (nextUnitTime / unitTime)) <= maxLabels) {
        return units[index + 1]
      }

      unitToReturn = units[index]
    }
  }

  return unitToReturn
}

export const getChartColorObject = (hexColor = '#1890FF') => ({
  backgroundColor: hexColor.concat('80'),
  borderColor: hexColor,
  borderWidth: 1.5
})
